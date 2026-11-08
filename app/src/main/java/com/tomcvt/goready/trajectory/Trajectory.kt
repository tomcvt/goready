package com.tomcvt.goready.trajectory

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max

/* ───────────────────────────── Model ───────────────────────────── */

/** Screen-frame point: +x right, +y down. */
data class Point2(val x: Float, val y: Float)

/**
 * Ordered polyline in "path units". points.first() = start, points.last() = end.
 * Closed shapes simply end on their start point.
 */
data class TargetPath(val id: String, val points: List<Point2>) {
    init { require(points.size >= 2) { "TargetPath needs at least 2 points" } }

    val start: Point2 get() = points.first()
    val end: Point2 get() = points.last()

    /** Larger bbox side; used to normalise match scores so thresholds are size-independent. */
    val extent: Float = run {
        val w = points.maxOf { it.x } - points.minOf { it.x }
        val h = points.maxOf { it.y } - points.minOf { it.y }
        max(w, h).coerceAtLeast(1e-6f)
    }
}

/** Hardcoded for now. Unit box, screen coords, clockwise on screen, strict start corner. */
object TargetPaths {
    val square = TargetPath(
        "square",
        listOf(
            Point2(0f, 0f),
            Point2(1f, 0f),
            Point2(1f, 1f),
            Point2(0f, 1f),
            Point2(0f, 0f))
    )
    val triangle = TargetPath(
        "triangle",
        listOf(
            Point2(0.5f, 0f),
            Point2(1f, 1f),
            Point2(0f, 1f),
            Point2(0.5f, 0f))
    )
    val defaults: Map<String, TargetPath> = listOf(square, triangle).associateBy { it.id }
}

/** Everything a Compose Canvas needs. Both lists are in path units. */
data class TrajectorySnapshot(
    val targetId: String,
    val targetPoints: List<Point2>,
    /** Starts at target.start; last element = current position. */
    val trackedPoints: List<Point2>,
    val isTracking: Boolean
) {
    val current: Point2 get() = trackedPoints.last()
}

/** score = mean ordered point distance / target extent. Lower is better. */
data class MatchResult(
    val targetId: String,
    val score: Float,
    val threshold: Float,
    val passed: Boolean
)

data class TrajectoryConfig(
    /** Rate at which trajectory points are emitted (sensor is still integrated at full rate). */
    val emitHz: Float = 5f,

    /** Path units per metre of phone travel. Default: a 15 cm gesture spans the unit box. */
    val scaleX: Float = 1f / 0.15f,
    val scaleY: Float = 1f / 0.15f,

    // ── LinearAccelMotionSource tuning (m/s², seconds) ──
    /** |a| below this is treated as 0 while integrating (noise gate). */
    val accelNoiseGate: Float = 0.12f,
    /** Exponential velocity decay (1/s). Bleeds integration bias; too high shrinks real strokes. */
    val velocityLeakPerSec: Float = 0.8f,
    /** Zero-velocity update: |a| below this for zuptHoldMs => velocity forced to 0. */
    val zuptAccelThreshold: Float = 0.25f,
    val zuptHoldMs: Long = 200,
    /** Clamp for dt so a scheduling hiccup can't inject a huge integration step. */
    val maxDtSec: Float = 0.05f,

    // ── Matching ──
    val resamplePoints: Int = 64,
    val passThreshold: Float = 0.2f
)

/* ─────────────────────── Motion source (swappable) ─────────────────────── */

/**
 * Turns raw sensor samples into a planar position relative to the tracking start,
 * in screen frame. Units are source-specific (metres for linear accel); the tracker
 * applies config.scaleX/Y to get path units. Pure Kotlin => unit-testable with recorded data.
 *
 * To try a gyro "pointer" later: implement this with Sensor.TYPE_GAME_ROTATION_VECTOR
 * and return yaw/pitch deltas as the position.
 */
interface MotionSource {
    val sensorType: Int
    fun reset()
    fun onSample(values: FloatArray, timestampNs: Long): Point2
}

/**
 * Double-integrates TYPE_LINEAR_ACCELERATION (gravity already removed by the platform fusion).
 * Frame: device x -> path x, device y -> path -y (phone held screen-facing, portrait).
 * Drift control: noise gate + velocity leak + ZUPT. ZUPT is also why polygons work well:
 * the natural pause at each corner zeroes velocity and stops drift accumulating across edges.
 */
class LinearAccelMotionSource(private val cfg: TrajectoryConfig) : MotionSource {
    override val sensorType: Int = Sensor.TYPE_LINEAR_ACCELERATION

    private var lastTsNs = 0L
    private var vx = 0f
    private var vy = 0f
    private var px = 0f
    private var py = 0f
    private var restSec = 0f

    override fun reset() {
        lastTsNs = 0L
        vx = 0f; vy = 0f; px = 0f; py = 0f; restSec = 0f
    }

    override fun onSample(values: FloatArray, timestampNs: Long): Point2 {
        if (lastTsNs == 0L) {
            lastTsNs = timestampNs
            return Point2(px, py)
        }
        val dt = ((timestampNs - lastTsNs) / 1e9f).coerceIn(0f, cfg.maxDtSec)
        lastTsNs = timestampNs

        var ax = values[0]
        var ay = -values[1] // device y is up, screen y is down
        val mag = hypot(ax, ay)

        if (mag < cfg.zuptAccelThreshold) restSec += dt else restSec = 0f
        if (mag < cfg.accelNoiseGate) { ax = 0f; ay = 0f }

        vx += ax * dt
        vy += ay * dt
        val leak = exp(-cfg.velocityLeakPerSec * dt)
        vx *= leak
        vy *= leak
        if (restSec >= cfg.zuptHoldMs / 1000f) { vx = 0f; vy = 0f }

        px += vx * dt
        py += vy * dt
        return Point2(px, py)
    }
}

/* ───────────────────────────── Matching ───────────────────────────── */

/**
 * Verify mode: arc-length resample both polylines to N points, mean ordered point distance,
 * normalised by target extent. Start is anchored (tracked starts at target.start) so there is no
 * translation normalisation; scale comes only from config.scaleX/Y. Direction/start corner strict.
 */
class PathMatcher(private val cfg: TrajectoryConfig) {

    fun verify(tracked: List<Point2>, target: TargetPath): MatchResult {
        if (tracked.size < 2) {
            return MatchResult(target.id, Float.POSITIVE_INFINITY, cfg.passThreshold, false)
        }
        val a = resample(tracked, cfg.resamplePoints)
        val b = resample(target.points, cfg.resamplePoints)
        var sum = 0f
        for (i in a.indices) sum += dist(a[i], b[i])
        val score = sum / a.size / target.extent
        return MatchResult(target.id, score, cfg.passThreshold, score <= cfg.passThreshold)
    }

    companion object {
        fun resample(pts: List<Point2>, n: Int): List<Point2> {
            require(n >= 2)
            val total = pathLength(pts)
            if (total <= 1e-6f) return List(n) { pts.first() }

            val step = total / (n - 1)
            val src = pts.toMutableList()
            val out = ArrayList<Point2>(n)
            out += src[0]
            var d = 0f
            var i = 1
            while (i < src.size) {
                val a = src[i - 1]
                val b = src[i]
                val seg = dist(a, b)
                if (seg > 0f && d + seg >= step) {
                    val t = (step - d) / seg
                    val q = Point2(a.x + t * (b.x - a.x), a.y + t * (b.y - a.y))
                    out += q
                    src.add(i, q) // q starts the next segment
                    d = 0f
                } else {
                    d += seg
                }
                i++
            }
            while (out.size < n) out += src.last() // float rounding can leave us 1 short
            return if (out.size > n) out.subList(0, n).toList() else out
        }

        private fun pathLength(pts: List<Point2>): Float {
            var l = 0f
            for (i in 1 until pts.size) l += dist(pts[i - 1], pts[i])
            return l
        }
    }
}

private fun dist(a: Point2, b: Point2) = hypot(a.x - b.x, a.y - b.y)

/* ───────────────────────────── Tracker ───────────────────────────── */

/**
 * Thin Android wrapper: owns the listener, decimates to emitHz, publishes snapshots.
 * Sensor callbacks arrive on the main thread (no Handler passed), so the StateFlow is updated there.
 *
 * Config is read at construction; to retune, build a new tracker (or a new source/matcher).
 */
class TrajectoryTracker(
    private val sensorManager: SensorManager,
    private val targets: Map<String, TargetPath> = TargetPaths.defaults,
    initialTargetId: String = targets.keys.first(),
    val config: TrajectoryConfig = TrajectoryConfig(),
    private val source: MotionSource = LinearAccelMotionSource(config),
    private val matcher: PathMatcher = PathMatcher(config),
    private val samplingPeriodUs: Int = SensorManager.SENSOR_DELAY_GAME,
    private val onStop: (MatchResult) -> Unit,
    private val onMatched: () -> Unit
) : SensorEventListener {

    init { require(initialTargetId in targets) { "Unknown target '$initialTargetId'" } }

    private val emitIntervalNs = (1_000_000_000.0 / config.emitHz).toLong()

    private var target: TargetPath = targets.getValue(initialTargetId)
    private var tracked: List<Point2> = listOf(target.start)
    private var latestRel = Point2(0f, 0f)
    private var lastEmitNs = 0L
    private var tracking = false

    private val _snapshot = MutableStateFlow(buildSnapshot())
    val snapshot: StateFlow<TrajectorySnapshot> = _snapshot.asStateFlow()

    val targetIds: Set<String> get() = targets.keys
    val currentTargetId: String get() = target.id

    /** Choose which path we verify against. Clears the current trajectory. */
    fun selectTarget(id: String) {
        require(id in targets) { "Unknown target '$id'" }
        target = targets.getValue(id)
        reset()
    }

    /**
     * Resets the trajectory and starts listening. Returns false if the sensor is missing
     * or registration failed (caller should fall back, like SwipeGate in the shake task).
     */
    fun startTracking(): Boolean {
        if (tracking) return true
        val sensor = sensorManager.getDefaultSensor(source.sensorType) ?: return false
        reset()
        tracking = sensorManager.registerListener(this, sensor, samplingPeriodUs)
        publish()
        return tracking
    }

    /** Stops listening, appends the final position, and verifies against the current target. */
    fun stopTracking(): MatchResult {
        if (tracking) {
            sensorManager.unregisterListener(this)
            tracking = false
            val last = toPathUnits(latestRel)
            if (last != tracked.last()) tracked = tracked + last
            publish()
        }
        val res = matcher.verify(tracked, target)
        onStop(res)
        if (res.passed) onMatched()
        return res
    }

    /** Discards the tracked path; position restarts at target.start. Safe while tracking. */
    fun reset() {
        source.reset()
        latestRel = Point2(0f, 0f)
        lastEmitNs = 0L
        tracked = listOf(target.start)
        publish()
    }

    override fun onSensorChanged(e: SensorEvent) {
        if (!tracking) return
        onSample(e.values, e.timestamp) // e.values is reused by the framework; not retained
    }

    override fun onAccuracyChanged(s: Sensor?, a: Int) {}

    /** Separated from onSensorChanged so tests can feed samples without a SensorEvent. */
    internal fun onSample(values: FloatArray, timestampNs: Long) {
        latestRel = source.onSample(values, timestampNs)
        if (lastEmitNs == 0L) { lastEmitNs = timestampNs; return }
        if (timestampNs - lastEmitNs < emitIntervalNs) return
        lastEmitNs = timestampNs
        tracked = tracked + toPathUnits(latestRel)
        publish()
    }

    private fun toPathUnits(rel: Point2) = Point2(
        target.start.x + rel.x * config.scaleX,
        target.start.y + rel.y * config.scaleY
    )

    private fun buildSnapshot() = TrajectorySnapshot(target.id, target.points, tracked, tracking)

    private fun publish() { _snapshot.value = buildSnapshot() }
}