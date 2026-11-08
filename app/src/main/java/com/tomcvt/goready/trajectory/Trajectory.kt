package com.tomcvt.goready.trajectory

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.hypot
import kotlin.math.max

private const val TAG = "TrajectoryTracker"
private const val TAG_FALLBACK = "TrajectoryFallback"

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

    /** Larger bbox side; normalises match scores so thresholds are size-independent. */
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
        listOf(Point2(0f, 0f), Point2(1f, 0f), Point2(1f, 1f), Point2(0f, 1f), Point2(0f, 0f))
    )
    val triangle = TargetPath(
        "triangle",
        listOf(Point2(0.5f, 0f), Point2(1f, 1f), Point2(0f, 1f), Point2(0.5f, 0f))
    )
    val defaults: Map<String, TargetPath> = listOf(square, triangle).associateBy { it.id }
}

/** Everything a Compose Canvas needs. Both point lists are in path units. */
data class TrajectorySnapshot(
    val targetId: String,
    val targetPoints: List<Point2>,
    /** Starts at target.start; last element = current position. */
    val trackedPoints: List<Point2>,
    val isTracking: Boolean,
    /** Which source is feeding the path right now ("arcore" / "tilt"). */
    val sourceName: String,
    /** Set when tracking stopped because every source failed. */
    val error: String? = null
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
    /** Rate at which trajectory points are emitted (sources may deliver faster). */
    val emitHz: Float = 5f,
    val resamplePoints: Int = 64,
    val passThreshold: Float = 0.2f
) {
    init { require(emitHz > 0f) }
}

/* ─────────────────────── Motion source abstraction ─────────────────────── */

interface MotionSink {
    /**
     * Position relative to the source's origin, in the source's native units, screen frame
     * (+x right, +y down). May be called from any thread.
     */
    fun onPosition(position: Point2, timestampNs: Long)

    /** The source died after a successful start() (e.g. ARCore lost the camera or never tracked). */
    fun onFailure(reason: String)

    /** A FallbackMotionSource switched to another source; the old trajectory is meaningless. */
    fun onSourceChanged(name: String) {}
}

interface MotionSource {
    val name: String

    /** Path units per native unit. The tracker applies these when converting positions. */
    val scaleX: Float
    val scaleY: Float

    /** @return false if this source cannot run right now (caller may try another). */
    fun start(sink: MotionSink): Boolean

    /** Make the current position the new origin. */
    fun resetOrigin()

    fun stop()
}

/**
 * Tries sources in order. If one refuses to start, or fails later (e.g. ARCore never reaches
 * TRACKING), moves on to the next and notifies the sink via onSourceChanged().
 */
class FallbackMotionSource(private val sources: List<MotionSource>) : MotionSource {
    init { require(sources.isNotEmpty()) }

    private val lock = Any()
    @Volatile private var activeIndex = -1
    private var downstream: MotionSink? = null

    private val current: MotionSource get() = sources[activeIndex.coerceAtLeast(0)]
    override val name: String get() = current.name
    override val scaleX: Float get() = current.scaleX
    override val scaleY: Float get() = current.scaleY

    override fun start(sink: MotionSink): Boolean = synchronized(lock) {
        downstream = sink
        startFrom(0)
    }

    override fun resetOrigin() { if (activeIndex >= 0) sources[activeIndex].resetOrigin() }

    override fun stop() {
        val toStop: MotionSource?
        synchronized(lock) {
            toStop = if (activeIndex >= 0) sources[activeIndex] else null
            activeIndex = -1
            downstream = null
        }
        toStop?.stop() // outside the lock: a source may join its worker thread here
    }

    /** Caller holds [lock]. */
    private fun startFrom(from: Int): Boolean {
        for (i in from until sources.size) {
            Log.d(TAG_FALLBACK, "trying source '${sources[i].name}'")
            activeIndex = i // set first so the relay accepts callbacks fired during start()
            if (sources[i].start(Relay(i))) {
                Log.d(TAG_FALLBACK, "source '${sources[i].name}' started")
                return true
            }
            Log.w(TAG_FALLBACK, "source '${sources[i].name}' refused to start")
            sources[i].stop()
        }
        activeIndex = -1
        Log.e(TAG_FALLBACK, "no motion source could start")
        return false
    }

    private fun failOver(failedIndex: Int, reason: String) {
        Log.e(TAG_FALLBACK, "source '${sources[failedIndex].name}' failed: $reason")
        val (sink, started) = synchronized(lock) {
            if (failedIndex != activeIndex) return
            val s = downstream ?: return
            sources[failedIndex].stop()
            s to startFrom(failedIndex + 1)
        }
        if (started) {
            Log.w(TAG_FALLBACK, "switched to source '${name}'")
            sink.onSourceChanged(name)
        } else {
            Log.e(TAG_FALLBACK, "all motion sources failed")
            sink.onFailure(reason)
        }
    }

    private inner class Relay(private val index: Int) : MotionSink {
        override fun onPosition(position: Point2, timestampNs: Long) {
            if (index == activeIndex) downstream?.onPosition(position, timestampNs)
        }

        override fun onFailure(reason: String) = failOver(index, reason)
    }
}

/* ───────────────────────────── Matching ───────────────────────────── */

/**
 * Verify mode: arc-length resample both polylines to N points, mean ordered point distance,
 * normalised by target extent. The tracked path starts at target.start so there is no translation
 * normalisation; scale comes only from the sources' scaleX/Y. Start corner and direction are strict.
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
 * Owns a MotionSource, decimates its positions to emitHz, publishes snapshots, verifies on stop.
 *
 * Threading: sources may call in from their own threads (ARCore worker, main-thread sensor
 * callbacks), so all state is guarded by [lock]. Source start/stop are always called OUTSIDE the
 * lock (a source's stop() may join a thread that is blocked waiting for this lock).
 */
class TrajectoryTracker(
    private val source: MotionSource,
    private val targets: Map<String, TargetPath> = TargetPaths.defaults,
    initialTargetId: String = targets.keys.first(),
    val config: TrajectoryConfig = TrajectoryConfig(),
    private val matcher: PathMatcher = PathMatcher(config),
    private var onTrackingStopped: (MatchResult) -> Unit,
    private var onMatch: () -> Unit
) : MotionSink {

    init { require(initialTargetId in targets) { "Unknown target '$initialTargetId'" } }

    private val lock = Any()
    private val emitIntervalNs = (1_000_000_000.0 / config.emitHz).toLong()

    private var target: TargetPath = targets.getValue(initialTargetId)
    private var tracked: List<Point2> = listOf(target.start)
    private var latestRel = Point2(0f, 0f)
    private var lastEmitNs = 0L
    private var tracking = false
    private var error: String? = null

    private val _snapshot = MutableStateFlow(snapshotLocked())
    val snapshot: StateFlow<TrajectorySnapshot> = _snapshot.asStateFlow()

    val targetIds: Set<String> get() = targets.keys
    val currentTargetId: String get() = synchronized(lock) { target.id }

    /** Choose which path we verify against. Clears the current trajectory. */
    fun selectTarget(id: String) {
        require(id in targets) { "Unknown target '$id'" }
        Log.d(TAG, "selectTarget: $id")
        synchronized(lock) {
            target = targets.getValue(id)
            resetLocked()
        }
        source.resetOrigin()
    }

    /**
     * Resets the trajectory and starts the source chain. Returns false if no source could start
     * (caller should fall back, like SwipeGate in the shake task). Note that true does not mean
     * the first source survives: the chain can still fail over asynchronously (see sourceName).
     */
    fun startTracking(): Boolean {
        synchronized(lock) {
            if (tracking) return true
            error = null
            resetLocked()
            tracking = true
            publishLocked()
        }
        Log.d(TAG, "startTracking: source='${source.name}'")
        val ok = source.start(this)
        if (!ok) {
            Log.e(TAG, "startTracking failed: no usable motion source")
            synchronized(lock) {
                tracking = false
                error = "No usable motion source"
                publishLocked()
            }
        }
        return ok
    }

    /** Stops the source, appends the final position, and verifies against the current target. */
    fun stopTracking(): MatchResult {
        val wasTracking = synchronized(lock) {
            val w = tracking
            tracking = false
            w
        }
        if (wasTracking) source.stop()
        val result = synchronized(lock) {
            if (wasTracking) {
                val last = toPathUnits(latestRel)
                if (last != tracked.last()) tracked = tracked + last
            }
            publishLocked()
            matcher.verify(tracked, target)
        }
        onTrackingStopped(result)
        if (result.passed) {
            Log.d(TAG, "match passed; invoking onMatch()")
            onMatch()
        }
        Log.d(
            TAG,
            "stopTracking: wasTracking=$wasTracking target=${result.targetId} " +
                    "score=${result.score} passed=${result.passed}"
        )
        return result
    }

    /** Discards the tracked path; position restarts at target.start. Safe while tracking. */
    fun reset() {
        Log.d(TAG, "reset")
        synchronized(lock) { resetLocked() }
        source.resetOrigin()
    }

    /* ── MotionSink ── */

    override fun onPosition(position: Point2, timestampNs: Long) {
        synchronized(lock) {
            if (!tracking) return@synchronized
            latestRel = position
            if (lastEmitNs == 0L) { lastEmitNs = timestampNs; return@synchronized }
            if (timestampNs - lastEmitNs < emitIntervalNs) return@synchronized
            lastEmitNs = timestampNs
            tracked = tracked + toPathUnits(position)
            publishLocked()
        }
    }

    override fun onFailure(reason: String) {
        Log.e(TAG, "motion source failed: $reason")
        synchronized(lock) {
            tracking = false
            error = reason
            publishLocked()
        }
    }

    override fun onSourceChanged(name: String) {
        // Different coordinate system / origin: start the trajectory over.
        Log.w(TAG, "motion source changed to '$name'; resetting trajectory")
        synchronized(lock) { resetLocked() }
    }

    /* ── internals (lock held) ── */

    private fun resetLocked() {
        latestRel = Point2(0f, 0f)
        lastEmitNs = 0L
        tracked = listOf(target.start)
        publishLocked()
    }

    private fun toPathUnits(rel: Point2) = Point2(
        target.start.x + rel.x * source.scaleX,
        target.start.y + rel.y * source.scaleY
    )

    private fun snapshotLocked() =
        TrajectorySnapshot(target.id, target.points, tracked, tracking, source.name, error)

    private fun publishLocked() { _snapshot.value = snapshotLocked() }
}