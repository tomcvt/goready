package com.tomcvt.goready.trajectory.composables

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tomcvt.goready.trajectory.Point2
import com.tomcvt.goready.trajectory.TrajectorySnapshot
import kotlin.math.max

/**
 * Draws a [TrajectorySnapshot] on a canvas:
 *  - the target trajectory in grey,
 *  - the tracked trajectory in green,
 *  - a dot at the target start and at the current tracked position.
 *
 * Path units are mapped to pixels so that the union of both trajectories fits the canvas,
 * preserving aspect ratio and leaving [padding] free space around it.
 */
@Composable
fun TrajectoryTrackerSnapshotRenderBox(
    snapshot: TrajectorySnapshot,
    modifier: Modifier = Modifier,
    targetColor: Color = Color(0xFF9E9E9E),
    trackedColor: Color = Color(0xFF2E7D32),
    targetStrokeWidth: Dp = 4.dp,
    trackedStrokeWidth: Dp = 4.dp,
    padding: Dp = 24.dp
) {
    Canvas(modifier = modifier) {
        val target = snapshot.targetPoints.filterFinite()
        val tracked = snapshot.trackedPoints.filterFinite()
        if (target.isEmpty() && tracked.isEmpty()) return@Canvas

        val bounds = pathBounds(target + tracked) ?: return@Canvas
        val extent = max(bounds.width, bounds.height).coerceAtLeast(1e-6f)

        val paddingPx = padding.toPx()
        val available = size.minDimension - paddingPx * 2f
        if (available <= 0f) return@Canvas

        val scale = available / extent
        val center = Offset(size.width / 2f, size.height / 2f)

        fun Point2.toCanvas() = Offset(
            x = center.x + (x - bounds.centerX) * scale,
            y = center.y + (y - bounds.centerY) * scale
        )

        // Target path, always underneath the tracked one.
        target.toPath()?.let { path ->
            drawPath(
                path = path,
                color = targetColor,
                style = Stroke(
                    width = targetStrokeWidth.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // Tracked path.
        tracked.toPath()?.let { path ->
            drawPath(
                path = path,
                color = trackedColor,
                style = Stroke(
                    width = trackedStrokeWidth.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // Start of the target and current position of the track.
        target.firstOrNull()?.let {
            drawCircle(color = targetColor, radius = 5.dp.toPx(), center = it.toCanvas())
        }
        tracked.lastOrNull()?.let {
            drawCircle(color = trackedColor, radius = 6.dp.toPx(), center = it.toCanvas())
        }
    }
}

private data class Bounds(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
) {
    val width: Float get() = maxX - minX
    val height: Float get() = maxY - minY
    val centerX: Float get() = (minX + maxX) / 2f
    val centerY: Float get() = (minY + maxY) / 2f
}

private fun pathBounds(points: List<Point2>): Bounds? {
    if (points.isEmpty()) return null
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE
    for (p in points) {
        minX = minOf(minX, p.x)
        minY = minOf(minY, p.y)
        maxX = maxOf(maxX, p.x)
        maxY = maxOf(maxY, p.y)
    }
    return Bounds(minX, minY, maxX, maxY)
}

private fun List<Point2>.filterFinite(): List<Point2> =
    filter { it.x.isFinite() && it.y.isFinite() }

private fun List<Point2>.toPath(): Path? {
    if (isEmpty()) return null
    val path = Path()
    val first = first()
    path.moveTo(first.x, first.y)
    for (i in 1 until size) {
        path.lineTo(this[i].x, this[i].y)
    }
    return path
}
