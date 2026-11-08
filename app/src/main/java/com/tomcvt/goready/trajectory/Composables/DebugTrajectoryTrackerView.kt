package com.tomcvt.goready.trajectory.composables

import android.hardware.Sensor
import android.hardware.SensorManager
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tomcvt.goready.trajectory.MatchResult
import com.tomcvt.goready.trajectory.TrajectoryTracker
import java.util.Locale

/**
 * Full-screen debug view for [TrajectoryTracker].
 *
 * Top left: target path selector. Middle: live [TrajectoryTrackerSnapshotRenderBox].
 * Bottom: start/stop toggle plus a reset button. Stopping tracking opens a dialog
 * with the [MatchResult] returned by the matcher.
 */
@Composable
fun DebugTrajectoryTrackerView(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(SensorManager::class.java) }
    var matchResult by remember { mutableStateOf<MatchResult?>(null) }
    val tracker = remember(sensorManager) {
        TrajectoryTracker(
            sensorManager = sensorManager,
            onStop = { matchResult = it },
            onMatched = {}
        )
    }
    val targetIds = remember(tracker) { tracker.targetIds.toList() }
    val snapshot by tracker.snapshot.collectAsState()

    val sensorPresent = remember {
        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null
    }
    var targetMenuExpanded by remember { mutableStateOf(false) }
    var startFailed by remember { mutableStateOf(false) }

    // Never leave the sensor listener registered behind the view.
    DisposableEffect(tracker) {
        onDispose {
            if (tracker.snapshot.value.isTracking) tracker.stopTracking()
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Button(
                        onClick = { targetMenuExpanded = true },
                        enabled = !snapshot.isTracking
                    ) {
                        Text("Target: ${snapshot.targetId}")
                    }
                    DropdownMenu(
                        expanded = targetMenuExpanded,
                        onDismissRequest = { targetMenuExpanded = false }
                    ) {
                        targetIds.forEach { id ->
                            DropdownMenuItem(
                                text = { Text(id) },
                                onClick = {
                                    tracker.selectTarget(id)
                                    targetMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            if (!sensorPresent || startFailed) {
                Text(
                    text = "Linear acceleration sensor unavailable on this device.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (snapshot.isTracking) {
                            tracker.stopTracking() // onStop opens the result dialog
                        } else {
                            startFailed = !tracker.startTracking()
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
                        tracker.reset()
                        startFailed = false
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
