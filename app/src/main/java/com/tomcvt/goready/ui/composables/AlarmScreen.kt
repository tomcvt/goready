package com.tomcvt.goready.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sqrt

@Composable
fun SimpleAlarmScreen(
    alarmId: Long,
    onStopAlarm: () -> Unit,
    modifier: Modifier = Modifier
) {
    // State to track the distance of the swipe from center
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    val threshold = 300f // Distance in pixels to trigger stop

    var interactionKey by remember { mutableStateOf(0L) }

    LaunchedEffect(interactionKey) {
        kotlinx.coroutines.delay(15000L)
        onStopAlarm()
    }


    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        interactionKey++
                    }
                }
            }
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Card with ID and Label
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ID: #$alarmId",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Alarm",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Bottom Circular Swipe Pane
        Box(
            modifier = Modifier
                .padding(bottom = 64.dp)
                .size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            // Static outer ring (The Threshold Boundary)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            )

            // The draggable "Snooze/Stop" handle
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.toInt(), offsetY.toInt()) }
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y

                                // Calculate distance from center (0,0)
                                val distance = sqrt(offsetX * offsetX + offsetY * offsetY)
                                if (distance > threshold) {
                                    onStopAlarm()
                                }
                            },
                            onDragEnd = {
                                // Snap back to center if threshold not met
                                offsetX = 0f
                                offsetY = 0f
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Stop",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = "Swipe to Stop",
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun TestAlarmScreen(
    alarmId: Long,
    onStopAlarm: () -> Unit,
    modifier: Modifier = Modifier
) {
    var interactionKey by remember { mutableStateOf(0L) }
 /*
    LaunchedEffect(interactionKey) {
        kotlinx.coroutines.delay(10000L)
        onStopAlarm()
    }
*/
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(alarmId.toString(), fontSize = 32.sp)
            Button(
                onClick = onStopAlarm,
                modifier = Modifier.size(200.dp)
            ) {
                Text("STOP", fontSize = 32.sp)
            }
        }
    }
}

@Composable
fun DebugTextAlarmScreen(
    text: String,
    onStopAlarm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        TextAlarmScreen(
            text = text,
            onStopAlarm = onStopAlarm,
            modifier = modifier
        )
        SimpleDeleteButton(
            onDelete = onStopAlarm,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }

}


@Composable
fun TextAlarmScreen(
    text: String,
    onStopAlarm: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentText by remember { mutableStateOf("") }
    var interactionKey by remember { mutableStateOf(0L) }
    LaunchedEffect(interactionKey) {
        kotlinx.coroutines.delay(10000L)
        onStopAlarm()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(text, fontSize = 32.sp)
            }

            TextInputCard(
                onTextChange = {currentText = it
                                interactionKey++
                               if (currentText == text) {onStopAlarm()}
                               },
                onFocusLost = {},
                placeholder = "Type your motto!"
            )
        }
    }
}
