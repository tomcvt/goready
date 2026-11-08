package com.tomcvt.goready.trajectory.composables

import android.hardware.SensorManager
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tomcvt.goready.trajectory.ArCoreMotionSource
import com.tomcvt.goready.trajectory.MatchResult
import com.tomcvt.goready.trajectory.TargetPaths
import com.tomcvt.goready.trajectory.TiltMotionSource
import com.tomcvt.goready.trajectory.TrajectoryTracker
import java.util.Locale

private const val TAG = "DebugTrajectoryTracker"

private enum class DebugMotionSource(val label: String) {
    ARCORE("arcore"),
    TILT("tilt")
}

/**
 * Full-screen debug view for [TrajectoryTracker].
 *
 * Top: target path selector, motion source selector and close button.
 * Middle: live [TrajectoryTrackerSnapshotRenderBox].
 * Bottom: start/stop toggle plus a reset button. Stopping tracking opens a dialog
 * with the [MatchResult] returned by the matcher.
 *
 * Runtime failures are logged under the tags "DebugTrajectoryTracker",
 * "TrajectoryTracker", "TrajectoryFallback", "ArCoreMotionSource" and "TiltMotionSource",
 * and the last tracker error is shown in the UI.
 */
@Composable
fun DebugTrajectoryTrackerView(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(SensorManager::class.java) }

    var selectedSource by remember { mutableStateOf(DebugMotionSource.ARCORE) }
    var selectedTargetId by remember { mutableStateOf(TargetPaths.defaults.keys.first()) }
    var matchResult by remember { mutableStateOf<MatchResult?>(null) }
    var targetMenuExpanded by remember { mutableStateOf(false) }
    var sourceMenuExpanded by remember { mutableStateOf(false) }

    // The tracker owns its source, so switching the source recreates both.
    val motionSource = remember(selectedSource) {
        when (selectedSource) {
            DebugMotionSource.ARCORE -> ArCoreMotionSource(context.applicationContext)
            DebugMotionSource.TILT -> TiltMotionSource(sensorManager)
        }
    }
    val tracker = remember(motionSource) {
        TrajectoryTracker(
            source = motionSource,
            initialTargetId = selectedTargetId,
            onTrackingStopped = { result ->
                Log.d(
                    TAG,
                    "onTrackingStopped: target=${result.targetId} " +
                            "score=${formatScore(result.score)} passed=${result.passed}"
                )
            },
            onMatch = { Log.d(TAG, "onMatch: trajectory matched the target") }
        )
    }
    val targetIds = remember(tracker) { tracker.targetIds.toList() }
    val snapshot by tracker.snapshot.collectAsState()

    LaunchedEffect(snapshot.error) {
        snapshot.error?.let { Log.e(TAG, "tracker error: $it") }
    }

    // Never leave the sensor / ARCore worker registered behind the view.
    DisposableEffect(tracker) {
        onDispose {
            Log.d(TAG, "dispose: stopping tracker and source")
            if (tracker.snapshot.value.isTracking) tracker.stopTracking()
            // Also covers asynchronous failures: the tracker already reports isTracking=false,
            // but the source may still hold a sensor listener / ARCore worker.
            motionSource.stop()
        }
    }

    BackHandler(onBack = onDismiss)

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    Button(
                        onClick = { targetMenuExpanded = true },
                        enabled = !snapshot.isTracking,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Target: ${snapshot.targetId}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    DropdownMenu(
                        expanded = targetMenuExpanded,
                        onDismissRequest = { targetMenuExpanded = false }
                    ) {
                        targetIds.forEach { id ->
                            DropdownMenuItem(
                                text = { Text(id) },
                                onClick = {
                                    Log.d(TAG, "target -> $id")
                                    selectedTargetId = id
                                    tracker.selectTarget(id)
                                    targetMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    Button(
                        onClick = { sourceMenuExpanded = true },
                        enabled = !snapshot.isTracking,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Source: ${selectedSource.label}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    DropdownMenu(
                        expanded = sourceMenuExpanded,
                        onDismissRequest = { sourceMenuExpanded = false }
                    ) {
                        DebugMotionSource.entries.forEach { source ->
                            DropdownMenuItem(
                                text = { Text(source.label) },
                                onClick = {
                                    if (!snapshot.isTracking && source != selectedSource) {
                                        Log.d(TAG, "motion source -> ${source.label}")
                                        selectedSource = source
                                        matchResult = null
                                    }
                                    sourceMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            TrajectoryTrackerSnapshotRenderBox(
                snapshot = snapshot,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Source: ${snapshot.sourceName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (snapshot.isTracking) {
                        "tracking · ${snapshot.trackedPoints.size} pts"
                    } else {
                        "idle · ${snapshot.trackedPoints.size} pts"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            snapshot.error?.let { error ->
                Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (snapshot.isTracking) {
                            val result = tracker.stopTracking()
                            Log.d(
                                TAG,
                                "stopTracking: target=${result.targetId} " +
                                        "score=${formatScore(result.score)} passed=${result.passed}"
                            )
                            matchResult = result
                        } else {
                            Log.d(TAG, "startTracking: source=${selectedSource.label}")
                            val started = tracker.startTracking()
                            if (!started) {
                                Log.e(
                                    TAG,
                                    "startTracking returned false for '${selectedSource.label}'; " +
                                            "see source logs for the reason"
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (snapshot.isTracking) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                ) {
                    Text(
                        text = if (snapshot.isTracking) "Stop tracking" else "Start tracking",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                IconButton(
                    onClick = {
                        Log.d(TAG, "reset")
                        tracker.reset()
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset trajectory",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }

    matchResult?.let { result ->
        AlertDialog(
            onDismissRequest = { matchResult = null },
            title = { Text("Match result") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Target: ${result.targetId}")
                    Text("Score: ${formatScore(result.score)}")
                    Text("Threshold: ${formatScore(result.threshold)}")
                    Text(
                        text = if (result.passed) "PASSED" else "FAILED",
                        color = if (result.passed) Color(0xFF2E7D32)
                        else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { matchResult = null }) { Text("OK") }
            }
        )
    }
}

private fun formatScore(score: Float): String =
    if (score.isFinite()) String.format(Locale.US, "%.3f", score) else "∞"
