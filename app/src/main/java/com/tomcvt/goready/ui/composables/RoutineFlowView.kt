package com.tomcvt.goready.ui.composables

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tomcvt.goready.data.StepWithDefinition
import com.tomcvt.goready.viewmodel.RoutineFlowViewModel
import com.tomcvt.goready.viewmodel.RoutinesViewModel
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun RoutineFlowContent(
    viewModel: RoutineFlowViewModel
) {
    val uiState by viewModel.flowUiState.collectAsState()
    val sessionState by viewModel.sessionState.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (uiState.launcherOverlay) {
            RoutineLauncherView(viewModel)
        } else {
            RoutineFlowView(viewModel)
        }
    }
}

@Composable
fun RoutineFlowView(
    viewModel: RoutineFlowViewModel
) {
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
                RoutineEntityDetails(currentRoutine)
                Card(
                    elevation = CardDefaults.cardElevation(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text("Step ${(sessionState?.stepNumber) ?: 0} of ${currentRoutineSteps.size}")
                }
                StepDetailsBox(
                    step = currentStep ?: emptyStep
                )
                Text(text = "Started: ${formatEpochMillisToHours(sessionState?.stepStartTime ?: 0)}")
                Text(text = "Step finish time: ${formatEpochMillisToHours(currentFinishTime ?: 0)}")
                StepTimer(viewModel, textStyle = MaterialTheme.typography.displayMedium)
            }
            Button(
                onClick = { viewModel.nextStep() },
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

    if (currentFinishTime != null) {
        LaunchedEffect(currentFinishTime) {
            while (true) {
                val remainingMs =
                    currentFinishTime!! - System.currentTimeMillis()

                if (remainingMs <= 0) {
                    timeText = "0:00"
                    break
                }

                timeText = formatMillis(remainingMs)
                delay(1000)
            }
        }
    }

    Text(text = timeText,
        style = textStyle
    )
}

@Composable
fun StepDetailsBox(
    step: StepWithDefinition
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                step.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                step.icon,
                modifier = Modifier.width(50.dp)
            )
            Text(
                step.length.toString() + "min",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.width(50.dp)
            )
        }
        Text(
            step.description,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth()
        )

    }
}

private val emptyStep = StepWithDefinition(
    id = 0,
    name = "",
    icon = "",
    length = 0,
    routineId = 0,
    stepType = "",
    stepNumber = 0,
    stepId = 0,
    description = ""
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


