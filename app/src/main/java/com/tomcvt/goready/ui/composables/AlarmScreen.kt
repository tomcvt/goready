package com.tomcvt.goready.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomcvt.goready.BuildConfig
import com.tomcvt.goready.constants.MathType
import com.tomcvt.goready.constants.TaskType
import kotlin.math.sqrt


@Composable
fun AlarmScreen(
    alarmId: Long,
    alarmName: String,
    taskType: TaskType,
    taskData: String?,
    onStopAlarm: () -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    var passedGate by remember { mutableStateOf(false) }
    if (!passedGate) {
        SimpleAlarmScreen(
            alarmName = alarmName,
            onSwiped = {passedGate = true}
        )
    } else {
        when (taskType) {
            TaskType.NONE -> {
                TestAlarmScreen(
                    alarmId = alarmId,
                    onStopAlarm = onStopAlarm,
                    modifier = Modifier.fillMaxSize()
                )
            }
            TaskType.TEXT -> {
                if (BuildConfig.IS_ALARM_TEST) {
                    DebugTextAlarmScreen(
                        text = taskData?: "Fallback",
                        onStopAlarm = onStopAlarm,
                        onInteraction = onInteraction,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    TextAlarmScreen(
                        text = taskData?: "Fallback",
                        onStopAlarm = onStopAlarm,
                        onInteraction = onInteraction,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            TaskType.TIMER -> {//TODO implement timer alarm screen
                SimpleAlarmScreen(
                    alarmName = "Temp Alarm",
                    onSwiped = onStopAlarm
                )
            }
            TaskType.COUNTDOWN -> {
                if (BuildConfig.IS_ALARM_TEST) {
                    DebugCountdownAlarmScreen(
                        number = taskData?: "7",
                        onStopAlarm = onStopAlarm,
                        onInteraction = onInteraction,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    CountdownAlarmScreen(
                        number = taskData?: "7",
                        onStopAlarm = onStopAlarm,
                        onInteraction = onInteraction,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            TaskType.MATH -> {//TODO implement math alarm screen
                if (BuildConfig.IS_ALARM_TEST) {
                    DebugMathAlarmScreen(
                        taskData = taskData?: "FIRST|1",
                        onStopAlarm = onStopAlarm,
                        onInteraction = onInteraction,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    MathAlarmScreen(
                        taskData = taskData?: "FIRST|1",
                        onStopAlarm = onStopAlarm,
                        onInteraction = onInteraction,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}


@Composable
fun SimpleAlarmScreen(
    alarmName: String,
    onSwiped: () -> Unit,
    modifier: Modifier = Modifier
) {
    // State to track the distance of the swipe from center
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    val threshold = 300f // Distance in pixels to trigger stop

    var interactionKey by remember { mutableStateOf(0L) }
/*
    LaunchedEffect(interactionKey) {
        kotlinx.coroutines.delay(15000L)
        onStopAlarm()
    }
*/

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
                    //TODO provide username
                    text = "Wakeup (here user name)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = alarmName,
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
            // outer ring
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            )

            // draggable circle
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
                                    onSwiped()
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
        SimpleDeleteButton(
            onDelete = onStopAlarm,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

@Composable
fun DebugTextAlarmScreen(
    text: String,
    onStopAlarm: () -> Unit,
    onInteraction : () -> Unit,
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
            onInteraction = onInteraction,
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
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentText by remember { mutableStateOf("") }
    var interactionKey by remember { mutableStateOf(0L) }
    var lastInteraction by remember { mutableStateOf(0L) }
    var showPopup by remember { mutableStateOf(false) }

    var focusRequester = remember { FocusRequester() }
    var keyboardController = LocalSoftwareKeyboardController.current


    LaunchedEffect(interactionKey) {
        if (System.currentTimeMillis() - lastInteraction > 2000L) {
            onInteraction()
            lastInteraction = System.currentTimeMillis()
        }
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
        verticalArrangement = Arrangement.Top
    ) {
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
                    //TODO consider variable style depending on message length
                    text = "Type your motto!",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.headlineMedium,
                    //fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextInputCard(
            onTextChange = {
                currentText = it
                interactionKey++
                if (currentText.equals(text, ignoreCase = true)) {
                    onStopAlarm()
                }
            },
            onFocusLost = {},
            placeholder = "Type your motto!",
            modifier = Modifier.fillMaxWidth()
                .padding(bottom = 64.dp)
                .focusRequester(focusRequester)
        )
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }
}

@Composable
fun CountdownAlarmScreen(
    number: String,
    onStopAlarm: () -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentCount by remember { mutableStateOf(Integer.parseInt(number))}
    var interactionKey by remember { mutableStateOf(0L) }
    var lastInteraction by remember { mutableStateOf(0L) }


    LaunchedEffect(interactionKey) {
        if (System.currentTimeMillis() - lastInteraction > 2000L) {
            onInteraction()
            lastInteraction = System.currentTimeMillis()
        }
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
                    text = "Press the button!",
                    style = MaterialTheme.typography.headlineLarge,
                    //fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Button(
            onClick = {currentCount--
                if (currentCount <= 0) {
                    onStopAlarm()
                }
            },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(Color.Red),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .size(200.dp)
                .padding(bottom = 64.dp)
        ) {
            Text(
                text = currentCount.toString(),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
fun DebugCountdownAlarmScreen(
    number: String,
    onStopAlarm: () -> Unit,
    onInteraction : () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        CountdownAlarmScreen(
            number = number,
            onStopAlarm = onStopAlarm,
            onInteraction = onInteraction,
            modifier = modifier
        )
        SimpleDeleteButton(
            onDelete = onStopAlarm,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

fun generateMathTaskList(taskData: String) : List<Pair<String,Int>> {
    val data = taskData.split("|")
    val mathType = MathType.valueOf(data[0])
    val number = data[1].toInt()
    return MathType.generateRandomTaskList(mathType, number)
}

@Composable
fun DebugMathAlarmScreen(
    taskData: String,
    onStopAlarm: () -> Unit,
    onInteraction : () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        MathAlarmScreen(
            taskData = taskData,
            onStopAlarm = onStopAlarm,
            onInteraction = onInteraction,
            modifier = modifier)
        SimpleDeleteButton(
            onDelete = onStopAlarm,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

@Composable
fun MathAlarmScreen(
    taskData: String,
    onStopAlarm: () -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val taskList by remember { mutableStateOf(generateMathTaskList(taskData)) }
    var currentTaskCount by remember { mutableStateOf(0) }
    var interactionKey by remember { mutableStateOf(0L) }
    var lastInteraction by remember { mutableStateOf(0L) }


    LaunchedEffect(interactionKey) {
        if (System.currentTimeMillis() - lastInteraction > 2000L) {
            onInteraction()
            lastInteraction = System.currentTimeMillis()
        }
    }
    LaunchedEffect(currentTaskCount) {
        if (currentTaskCount >= taskList.size) {
            onStopAlarm()
        }
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
                    text = "Solve the task!",
                    style = MaterialTheme.typography.headlineLarge,
                    //fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (currentTaskCount < taskList.size) {
            MathTaskScreen(
                taskPair = taskList[currentTaskCount],
                onCompletion = { currentTaskCount++ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 64.dp)
            )
        } else {
            currentTaskCount++
        }
    }
}

@Composable
fun MathTaskScreen(
    taskPair: Pair<String,Int>,
    onCompletion: () -> Unit,
    modifier: Modifier = Modifier
) {
    val solution = taskPair.second.toString()
    var currentText by remember { mutableStateOf("") }
    val checkSolution = {
        if (currentText.equals(solution, ignoreCase = true)) {
            currentText = ""
            onCompletion()
        }
    }
    LaunchedEffect(currentText) {
        checkSolution()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Box(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = taskPair.first,
                    style = MaterialTheme.typography.headlineMedium,
                    //fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        NumbersInput(
            value = currentText,
            onValueChange = { currentText = it },
            onFocusLost = {},
            placeholder = " ",
            modifier = Modifier.height(100.dp).width(200.dp),
            fontSize = 32.sp
        )
    }
}