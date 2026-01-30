package com.tomcvt.goready.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tomcvt.goready.viewmodel.RoutineFlowViewModel

@Composable
fun RoutineLauncherView(
    viewModel: RoutineFlowViewModel
) {
    val uiState by viewModel.flowUiState.collectAsState()
    RoutineDetailsBoxLauncher(viewModel)
}


@Composable
fun RoutineDetailsBoxLauncher(
    viewModel: RoutineFlowViewModel,
    //navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.flowUiState.collectAsState()
    val routineSteps by viewModel.launcherRoutineSteps.collectAsState()
    val routineEntity by viewModel.launcherRoutine.collectAsState()

    Box (
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable {},
        contentAlignment = Alignment.Center
    ) {
        Card(
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Box (
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Edit routine", style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    RoutineEntityDetails(routineEntity)
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(routineSteps.size) { index ->
                            val step = routineSteps[index]
                            StepRowCard(step)
                        }
                    }
                    Button(
                        onClick = { viewModel.launchRoutine(uiState.launcherRoutineId?: 0)
                                    viewModel.closeRoutineLauncher() },
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("Launch Routine")
                    }
                }
            }
        }
    }
}





