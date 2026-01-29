package com.tomcvt.goready.ui.composables

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.dp
import com.tomcvt.goready.viewmodel.RoutineFlowViewModel
import kotlinx.coroutines.delay

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
    val currentStep by viewModel.currentRoutine.collectAsState()
    val currentFinishTime by viewModel.currentStepFinishTime.collectAsState()



    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "Session state: ${sessionState}")
        Text(text = "Routine: ${currentRoutine}")
        Text(text = "Step: ${currentStep}")
        Text(text = "Step start time: ${sessionState?.startTime}")
        Text(text = "Step finish time: ${currentFinishTime}")
        StepTimer(viewModel)
    }

}

@Composable
fun StepTimer(
    viewModel: RoutineFlowViewModel
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

    Text(text = timeText)
}

fun formatMillis(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val rSeconds = seconds % 60
    return "%02d:%02d".format(minutes, rSeconds)
}
