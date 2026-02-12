package com.tomcvt.goready.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tomcvt.goready.BuildConfig
import com.tomcvt.goready.ads.ADMOB_ID_STANDARD_BOTTOM_BANNER
import com.tomcvt.goready.ads.ADMOB_ID_TEST_BANNER
import com.tomcvt.goready.ads.BottomBarAdView
import com.tomcvt.goready.constants.StepType
import com.tomcvt.goready.data.RoutineStatus
import com.tomcvt.goready.data.StepStatus
import com.tomcvt.goready.data.StepWithDefinition
import com.tomcvt.goready.viewmodel.RoutineFlowViewModel
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun RoutineFlowContent(
    viewModel: RoutineFlowViewModel,
    onClose: () -> Unit = {},
    onUserInitInteraction: () -> Unit = {}
) {
    val uiState by viewModel.flowUiState.collectAsState()
    val sessionState by viewModel.sessionState.collectAsState()
    var firstClicked by remember { mutableStateOf(false) }
    var interactionKey by remember { mutableStateOf(0L) }

    LaunchedEffect(interactionKey) {
        if (!firstClicked) {
            onUserInitInteraction()
            firstClicked = true
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial)
                            interactionKey++
                        }
                    }
                }
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                if (uiState.launcherOverlay) {
                    RoutineLauncherView(viewModel)
                } else {
                    RoutineFlowView(viewModel, Modifier.weight(1f), onClose)
                }
                BottomBarAdView(
                    adUnitId = ADMOB_ID_TEST_BANNER,
                    modifier = Modifier.fillMaxWidth()
                )
            }

        }
    }
}

@Composable
fun RoutineFlowView(
    viewModel: RoutineFlowViewModel,
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {}
) {
    val sessionState by viewModel.sessionState.collectAsState()
    val currentRoutine by viewModel.currentRoutine.collectAsState()
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (sessionState == null) {
            NoSessionActiveBox(onClose)
        }
        if (sessionState?.status == RoutineStatus.RUNNING) {
            if (sessionState?.stepStatus == StepStatus.AWAITING) {
                WaitingStepBox(viewModel)
            }
            if (sessionState?.stepStatus == StepStatus.RUNNING) {
                RunningStepBox(viewModel)
            }
            if (sessionState?.stepStatus == StepStatus.COMPLETED) {
                CompletedStepBox(viewModel)
            }
        }
        if (sessionState?.status == RoutineStatus.COMPLETED) {
            Text("Routine completed")
        }
    }
}

@Composable
fun NoSessionActiveBox(
    onClose: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "No routine is currently active.",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .padding(32.dp)
                        .align(Alignment.CenterHorizontally),
                    textAlign = TextAlign.Center
                )
            }
            Button(
                onClick = onClose,
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Close")
            }
        }
    }
}

@Composable
fun WaitingStepBox(viewModel: RoutineFlowViewModel) {
    val currentStep by viewModel.currentStep.collectAsState()
    var launched by remember { mutableStateOf(false) }


    LaunchedEffect(Unit) {
        delay(1000)
        launched = true
    }
    CardPopupAnimated(
        launched = launched
    ) {
        StepDetailsCard(
            step = currentStep ?: emptyStep
        )
    }

    Button(
        onClick = { viewModel.startStep() },
        modifier = Modifier.padding(16.dp)
    ) {
        Text("Start Step")
    }
}

@Composable
fun CompletedStepBox(viewModel: RoutineFlowViewModel) {
    val currentStep by viewModel.currentStep.collectAsState()
    var passedGate by remember { mutableStateOf(false) }
    var enabled by remember { mutableStateOf(false) }

    LaunchedEffect(passedGate) {
        delay(1000)
        enabled = true
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp)
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        if (!passedGate) {
            Text("Completed step: ${currentStep?.name}")
            Button(
                onClick = { passedGate = true },
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Next Step")
            }
        } else {
            Box (
                modifier = Modifier.fillMaxSize().padding(16.dp),

            ) {
                if (BuildConfig.DEBUG) {
                    Button (
                        onClick = { enabled = !enabled },
                        modifier = Modifier.padding(16.dp).align(Alignment.TopEnd)
                    ) {
                        Text("Toggle Enabled")
                    }
                }
                StepDetailsCard(
                    step = currentStep ?: emptyStep
                    , modifier = Modifier.align(Alignment.TopCenter)
                )
                SimpleTickCircleAnimations(
                    checked = enabled,
                    modifier = Modifier.padding(16.dp).align(Alignment.Center)
                )
                Button(
                    onClick = { viewModel.nextStep(); passedGate = false },
                    modifier = Modifier.padding(16.dp).align(Alignment.BottomCenter)
                ) {
                    Text(
                        "Next Step",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        }
    }
}


@Composable
fun RunningStepBox(viewModel: RoutineFlowViewModel) {
    val sessionState by viewModel.sessionState.collectAsState()
    val currentRoutine by viewModel.currentRoutine.collectAsState()
    val currentRoutineSteps by viewModel.currentRoutineSteps.collectAsState()
    val currentStep by viewModel.currentStep.collectAsState()
    val currentFinishTime by viewModel.currentStepFinishTime.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp)
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    RoutineEntityDetails(currentRoutine)
                    Text(
                        "Step ${((sessionState?.stepNumber) ?: -1) + 1} of ${currentRoutineSteps.size}",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                StepDetailsCard(
                    step = currentStep ?: emptyStep
                )
                Text(text = "Started: ${formatEpochMillisToHours(sessionState?.stepStartTime ?: 0)}",
                    style = MaterialTheme.typography.headlineSmall)
                Text(text = "Finish time: ${formatEpochMillisToHours(currentFinishTime ?: 0)}",
                    style = MaterialTheme.typography.headlineSmall)
                StepTimer(viewModel, textStyle = MaterialTheme.typography.displayLarge)
            }
            Button(
                onClick = { viewModel.finishStep() },
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Next Step")
            }
        }
    }
}

@Composable
fun StepTimer(
    viewModel: RoutineFlowViewModel,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.headlineMedium
) {
    val currentFinishTime by viewModel.currentStepFinishTime.collectAsState()
    var timeText by remember { mutableStateOf("") }
    var finished by remember { mutableStateOf(false) }


    if (currentFinishTime != null) {
        LaunchedEffect(currentFinishTime) {
            while (true) {
                val remainingMs =
                    currentFinishTime!! - System.currentTimeMillis()

                if (remainingMs <= 0) {
                    timeText = "0:00"
                    finished = true
                    break
                }

                timeText = formatMillis(remainingMs)
                delay(1000)
            }
        }
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = timeText,
            style = textStyle
        )
        if (finished) {
            Text(
                text = "Time's up!",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center)
        }
    }
}


@Composable
fun StepDetailsCardContent(
    step: StepWithDefinition,
    modifier: Modifier = Modifier
) {
    Box (
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        step.name,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        step.icon,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    step.length.toString() + "min",
                    style = MaterialTheme.typography.headlineLarge
                )
            }
            Text(
                step.description,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun StepDetailsCard(
    step: StepWithDefinition,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = modifier
            .fillMaxWidth()
    ) {
        StepDetailsCardContent(step)
    }
}

private val emptyStep = StepWithDefinition(
    id = 0,
    name = "",
    icon = "",
    length = 0,
    routineId = 0,
    stepType = StepType.NONE,
    stepNumber = 0,
    stepId = 0,
    description = "",
    updatable = false
)


fun formatMillis(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val rSeconds = seconds % 60
    return "%02d:%02d".format(minutes, rSeconds)
}

fun formatEpochMillisToHours(millis: Long): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = millis

    val hour = calendar.get(Calendar.HOUR_OF_DAY) // 0–23
    val minute = calendar.get(Calendar.MINUTE)
    val second = calendar.get(Calendar.SECOND)
    return String.format("%02d:%02d:%02d", hour, minute, second)
}


