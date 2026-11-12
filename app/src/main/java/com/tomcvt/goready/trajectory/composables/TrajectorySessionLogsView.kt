package com.tomcvt.goready.trajectory.composables

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tomcvt.goready.trajectory.TrajectorySessionLog
import java.util.Locale

/**
 * Read-only view of the position samples recorded for the current or most recent tracking
 * session. Selecting "Logs" in the debug view's options menu shows this screen.
 */
@Composable
fun TrajectorySessionLogsView(
    log: TrajectorySessionLog,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Logs",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = buildString {
                            append("Target: ").append(log.targetId)
                            append(" · Source: ").append(log.sourceName)
                            append(" · ").append(log.records.size).append(" samples")
                            if (log.isTracking) append(" · live")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close logs")
                }
            }

            if (log.records.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No positions recorded yet.\nStart tracking to record a session.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val firstTimestampNs = log.records.first().timestampNs
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(log.records) { index, record ->
                        val seconds =
                            (record.timestampNs - firstTimestampNs) / 1_000_000_000.0
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = 8.dp
                                )
                            ) {
                                Text(
                                    text = "#$index   t=${formatSeconds(seconds)} s",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "rel  (${formatCoord(record.rel.x)}, " +
                                            "${formatCoord(record.rel.y)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "path (${formatCoord(record.path.x)}, " +
                                            "${formatCoord(record.path.y)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatCoord(value: Float): String = String.format(Locale.US, "%+.3f", value)

private fun formatSeconds(value: Double): String = String.format(Locale.US, "%.3f", value)
