package com.tomcvt.goready.trajectory

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.exp
import kotlin.math.hypot

private const val TAG = "TiltMotionSource"

/**
 * Fallback source: the phone is a tilting tray, the ball rolls downhill.
 * Gravity is a stable absolute reference, so unlike double integration there is no drift.
 *
 * The ball lives directly in path units (scale defaults to 1) and starts at the origin,
 * which the tracker maps onto target.start.
 */
data class TiltSourceConfig(
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    /** Path units / s² per 1 g of tilt (relative to the neutral pose). */
    val accelGain: Float = 8f,
    /** Velocity decay (1/s). Higher = heavier "friction", easier to steer, slower. */
    val damping: Float = 2f,
    /** Bounce off the walls (0 = dead stop, 1 = perfectly elastic). */
    val restitution: Float = 0.2f,
    /** Walls at ±halfExtent around the start, in path units. */
    val halfExtent: Float = 1.5f,
    /** Accelerometer low-pass time constant; filters hand tremor. */
    val lowPassTauSec: Float = 0.08f,
    /** Tilt (in g) below which the ball does not move. ~0.02 g ≈ 1.1°. */
    val deadZoneG: Float = 0.015f,
    /**
     * The first calibrationMs of samples define the neutral pose, so the user may start from any
     * posture (flat on a table, held at an angle). The ball does not move during calibration.
     */
    val calibrationMs: Long = 300,
    val maxDtSec: Float = 0.05f
)

/** Pure Kotlin physics, unit-testable with synthetic samples. */
class TiltBall(private val cfg: TiltSourceConfig) {
    private var lastTs = 0L
    private var fx = 0f
    private var fy = 0f
    private var calibrated = false
    private var calibElapsedSec = 0f
    private var sumX = 0f
    private var sumY = 0f
    private var n = 0
    private var neutralX = 0f
    private var neutralY = 0f
    private var vx = 0f
    private var vy = 0f
    private var px = 0f
    private var py = 0f

    fun reset() {
        lastTs = 0L
        fx = 0f; fy = 0f
        calibrated = false; calibElapsedSec = 0f
        sumX = 0f; sumY = 0f; n = 0
        neutralX = 0f; neutralY = 0f
        vx = 0f; vy = 0f; px = 0f; py = 0f
    }

    /**
     * ax, ay: raw TYPE_ACCELEROMETER x/y (m/s², device frame: x right, y toward the top edge).
     * Returns the ball position in screen frame (+x right, +y down).
     */
    fun step(ax: Float, ay: Float, tsNs: Long): Point2 {
        if (lastTs == 0L) {
            lastTs = tsNs
            fx = ax; fy = ay
            return Point2(px, py)
        }
        val dt = ((tsNs - lastTs) / 1e9f).coerceIn(0f, cfg.maxDtSec)
        lastTs = tsNs

        val alpha = dt / (cfg.lowPassTauSec + dt)
        fx += alpha * (ax - fx)
        fy += alpha * (ay - fy)

        if (!calibrated) {
            calibElapsedSec += dt
            sumX += fx; sumY += fy; n++
            if (calibElapsedSec * 1000f >= cfg.calibrationMs && n > 0) {
                neutralX = sumX / n
                neutralY = sumY / n
                calibrated = true
            }
            return Point2(px, py)
        }

        // The accelerometer reads the reaction to gravity, so "downhill" is the opposite sign:
        // right edge down => ax < 0 => ball goes right (+x). Top edge down => ay < 0 => ball goes
        // up the screen (-y in screen frame), hence the sign flip on x only.
        var tx = -(fx - neutralX) / G
        var ty = (fy - neutralY) / G
        if (hypot(tx, ty) < cfg.deadZoneG) { tx = 0f; ty = 0f }

        vx += cfg.accelGain * tx * dt
        vy += cfg.accelGain * ty * dt
        val k = exp(-cfg.damping * dt)
        vx *= k
        vy *= k
        px += vx * dt
        py += vy * dt

        val h = cfg.halfExtent
        if (px > h) { px = h; vx = -vx * cfg.restitution } else if (px < -h) { px = -h; vx = -vx * cfg.restitution }
        if (py > h) { py = h; vy = -vy * cfg.restitution } else if (py < -h) { py = -h; vy = -vy * cfg.restitution }

        return Point2(px, py)
    }

    private companion object { const val G = 9.80665f }
}

/**
 * Assumes portrait / no display rotation. Callbacks arrive on the main thread (no Handler passed),
 * so [ball] is only ever touched from there; resetOrigin() just raises a flag.
 */
class TiltMotionSource(
    private val sensorManager: SensorManager,
    private val cfg: TiltSourceConfig = TiltSourceConfig(),
    private val samplingPeriodUs: Int = SensorManager.SENSOR_DELAY_GAME
) : MotionSource, SensorEventListener {

    override val name: String = "tilt"
    override val scaleX: Float get() = cfg.scaleX
    override val scaleY: Float get() = cfg.scaleY

    private val ball = TiltBall(cfg)
    @Volatile private var sink: MotionSink? = null
    @Volatile private var resetRequested = true

    override fun start(sink: MotionSink): Boolean {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensor == null) {
            Log.e(TAG, "start failed: no TYPE_ACCELEROMETER sensor on this device")
            return false
        }
        this.sink = sink
        resetRequested = true
        val ok = sensorManager.registerListener(this, sensor, samplingPeriodUs)
        if (!ok) {
            Log.e(TAG, "start failed: registerListener returned false")
            this.sink = null
        } else {
            Log.d(TAG, "started (samplingPeriodUs=$samplingPeriodUs)")
        }
        return ok
    }

    override fun resetOrigin() { resetRequested = true }

    override fun stop() {
        Log.d(TAG, "stop")
        sensorManager.unregisterListener(this)
        sink = null
    }

    override fun onSensorChanged(e: SensorEvent) {
        val s = sink ?: return
        try {
            if (resetRequested) { resetRequested = false; ball.reset() }
            s.onPosition(ball.step(e.values[0], e.values[1], e.timestamp), e.timestamp)
        } catch (t: Throwable) {
            Log.e(TAG, "onSensorChanged failed", t)
            s.onFailure("tilt: ${t.javaClass.simpleName}: ${t.message}")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}