package com.tomcvt.goready.ui.composables

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.tomcvt.goready.viewmodel.RoutineFlowViewModel

@Composable
fun RoutineLauncherView(
    viewModel: RoutineFlowViewModel,
    routineId: Long
) {


    Text("RoutineLauncherView: $routineId")
    Button(
        onClick = { viewModel.launchRoutine(routineId) }
    ) {
        Text("Launch Routine")
    }
}