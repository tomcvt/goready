package com.tomcvt.goready.trajectory

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.view.Surface
import android.util.Log
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState

/**
 * Source: ARCore camera pose (visual-inertial odometry), headless (no preview, no GL rendering).
 *
 * Native units are metres, measured in the display-oriented camera frame of the first TRACKING
 * frame (x right, y up of the screen), then flipped to screen frame (y down). scaleX/Y convert
 * metres to path units: the default maps a 15 cm movement onto the unit box.
 */
data class ArCoreSourceConfig(
    val scaleX: Float = 1f / 0.15f,
    val scaleY: Float = 1f / 0.15f,
    /**
     * If ARCore never reaches TRACKING within this time after the session started, the source
     * reports failure so the fallback chain can move on (dark room, blank surface, no motion).
     * VIO initialisation needs a little device motion, so don't set this too low.
     */
    val trackingTimeoutMs: Long = 5000,
    /** Portrait-locked assumption; pass the real values if you support rotation. */
    val displayRotation: Int = Surface.ROTATION_0,
    val displayWidthPx: Int = 1080,
    val displayHeightPx: Int = 1920
)

object ArCoreSupport {
    private const val TAG = "ArCoreSupport"

    /**
     * Call once early (e.g. Application.onCreate). The first checkAvailability() can return a
     * transient UNKNOWN_CHECKING while it queries; later calls hit the cached answer.
     */
    fun prewarm(context: Context) {
        ArCoreApk.getInstance().checkAvailability(context.applicationContext)
    }

    /** Supported device AND Google Play Services for AR installed and up to date. */
    fun isReady(context: Context): Boolean {
        val availability = ArCoreApk.getInstance().checkAvailability(context.applicationContext)
        if (availability != ArCoreApk.Availability.SUPPORTED_INSTALLED) {
            Log.w(TAG, "ARCore not ready: availability=$availability")
        }
        return availability == ArCoreApk.Availability.SUPPORTED_INSTALLED
    }
}

class ArCoreMotionSource(
    context: Context,
    private val cfg: ArCoreSourceConfig = ArCoreSourceConfig()
) : MotionSource {

    private companion object { const val TAG = "ArCoreMotionSource" }

    override val name: String = "arcore"
    override val scaleX: Float get() = cfg.scaleX
    override val scaleY: Float get() = cfg.scaleY

    private val appContext = context.applicationContext

    /** One per start(); lets a stale worker (still closing its session) exit without touching a newer run. */
    private class Run(val sink: MotionSink) {
        @Volatile var active = true
        @Volatile var resetRequested = true
    }

    @Volatile private var run: Run? = null
    private var worker: Thread? = null

    /**
     * Synchronous checks only (permission, device support, ARCore installed). Everything that can
     * still go wrong (session creation, camera busy, no tracking) is reported later through
     * sink.onFailure so FallbackMotionSource can move on to the next source.
     */
    override fun start(sink: MotionSink): Boolean {
        if (run?.active == true) {
            Log.d(TAG, "start ignored: already running")
            return true
        }
        if (appContext.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "start failed: CAMERA permission not granted")
            return false
        }
        if (!ArCoreSupport.isReady(appContext)) {
            Log.e(TAG, "start failed: ARCore is not supported/installed")
            return false
        }

        Log.d(TAG, "starting ARCore worker")
        val r = Run(sink)
        val previous = worker
        run = r
        worker = Thread({
            // The previous session may still be closing (close() can take seconds) and holds the camera.
            runCatching { previous?.join() }
            loop(r)
        }, "trajectory-arcore").apply { isDaemon = true; start() }
        return true
    }

    override fun resetOrigin() { run?.resetRequested = true }

    /** Non-blocking: the worker pauses/closes the session itself, off the calling thread. */
    override fun stop() {
        Log.d(TAG, "stop requested")
        run?.active = false
        run = null
    }

    private fun loop(r: Run) {
        val gl = HeadlessGl()
        var session: Session? = null
        var failure: String? = null
        try {
            if (!r.active) return
            gl.init()
            Log.d(TAG, "EGL context initialised")

            // update() needs a GL context on this thread and a camera texture, even if we never draw.
            session = Session(appContext).also { s ->
                val config = Config(s)
                config.setUpdateMode(Config.UpdateMode.BLOCKING)
                config.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED)
                config.setLightEstimationMode(Config.LightEstimationMode.DISABLED)
                config.setDepthMode(Config.DepthMode.DISABLED)
                config.setInstantPlacementMode(Config.InstantPlacementMode.DISABLED)
                s.configure(config)
                s.setCameraTextureName(gl.oesTexture)
                s.setDisplayGeometry(cfg.displayRotation, cfg.displayWidthPx, cfg.displayHeightPx)
                s.resume()
            }
            Log.d(TAG, "ARCore session created and resumed")

            val startNs = System.nanoTime()
            var everTracked = false
            var lastFrameTs = 0L
            var originInverse: Pose? = null

            while (r.active) {
                // Blocking mode returns the latest frame after ~66 ms even if no new image arrived.
                val frame = session!!.update()
                val camera = frame.camera

                if (camera.trackingState != TrackingState.TRACKING) {
                    if (!everTracked && (System.nanoTime() - startNs) / 1_000_000 > cfg.trackingTimeoutMs) {
                        failure = "ARCore not tracking after ${cfg.trackingTimeoutMs} ms (${camera.trackingFailureReason})"
                        Log.e(TAG, failure)
                        break
                    }
                    continue
                }
                val ts = frame.timestamp
                if (ts == 0L || ts == lastFrameTs) continue
                lastFrameTs = ts
                if (!everTracked) Log.d(TAG, "camera tracking acquired")
                everTracked = true

                val pose = camera.displayOrientedPose
                if (r.resetRequested || originInverse == null) {
                    originInverse = pose.inverse()
                    r.resetRequested = false
                }
                val local = originInverse.compose(pose) // current pose expressed in the origin frame
                r.sink.onPosition(Point2(local.tx(), -local.ty()), ts)
            }
        } catch (t: Throwable) {
            failure = "${t.javaClass.simpleName}: ${t.message}"
            Log.e(TAG, "ARCore worker crashed", t)
        } finally {
            runCatching { session?.pause() }
            runCatching { session?.close() } // several seconds; fine, we're on the worker
            gl.release()
            Log.d(TAG, "ARCore worker finished")
        }

        if (failure != null && r.active) {
            Log.e(TAG, "reporting failure to tracker: $failure")
            r.active = false
            r.sink.onFailure(failure)
        }
    }
}

/** Minimal EGL 2.0 context + 1x1 pbuffer + an OES texture for ARCore. Must live on the worker thread. */
private class HeadlessGl {
    private var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var context: EGLContext = EGL14.EGL_NO_CONTEXT
    private var surface: EGLSurface = EGL14.EGL_NO_SURFACE

    var oesTexture = 0
        private set

    fun init() {
        display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        check(display != EGL14.EGL_NO_DISPLAY) { "No EGL display" }
        val version = IntArray(2)
        check(EGL14.eglInitialize(display, version, 0, version, 1)) { "eglInitialize failed" }

        val attrs = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
            EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val count = IntArray(1)
        check(EGL14.eglChooseConfig(display, attrs, 0, configs, 0, 1, count, 0) && count[0] > 0) { "No EGL config" }

        context = EGL14.eglCreateContext(
            display, configs[0], EGL14.EGL_NO_CONTEXT,
            intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0
        )
        surface = EGL14.eglCreatePbufferSurface(
            display, configs[0],
            intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE), 0
        )
        check(EGL14.eglMakeCurrent(display, surface, surface, context)) { "eglMakeCurrent failed" }

        val tex = IntArray(1)
        GLES20.glGenTextures(1, tex, 0)
        oesTexture = tex[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTexture)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)
    }

    fun release() {
        if (display != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            if (surface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, surface)
            if (context != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, context)
            // Deliberately no eglTerminate(): the default display is process-wide and other EGL
            // users in the app (GLSurfaceView, video, etc.) would lose it.
        }
        display = EGL14.EGL_NO_DISPLAY
        context = EGL14.EGL_NO_CONTEXT
        surface = EGL14.EGL_NO_SURFACE
        oesTexture = 0
    }
}

/** Wiring: ARCore first, tilt if ARCore is unavailable at start or fails afterwards. */
object TrajectorySources {
    fun arCoreWithTiltFallback(
        context: Context,
        ar: ArCoreSourceConfig = ArCoreSourceConfig(),
        tilt: TiltSourceConfig = TiltSourceConfig()
    ): MotionSource {
        val app = context.applicationContext
        val sm = app.getSystemService(SensorManager::class.java)
        return FallbackMotionSource(listOf(ArCoreMotionSource(app, ar), TiltMotionSource(sm, tilt)))
    }

    fun tiltOnly(context: Context, tilt: TiltSourceConfig = TiltSourceConfig()): MotionSource =
        TiltMotionSource(context.applicationContext.getSystemService(SensorManager::class.java), tilt)
}