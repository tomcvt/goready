package com.tomcvt.goready.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tomcvt.goready.viewmodel.RoutineFlowViewModel

@Composable
fun RoutineLauncherView(
    viewModel: RoutineFlowViewModel,
    routineId: Long
) {
    val sessionId by viewModel.selectedSessionId.collectAsState()
    val uiState by viewModel.flowUiState.collectAsState()

    Box (
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Selected session: $sessionId")
            Text("RoutineLauncherView: $routineId")
            Button(
                onClick = { viewModel.launchRoutine(uiState.launcherRoutineId) }
            ) {
                Text("Launch Routine")
            }
        }
    }
}