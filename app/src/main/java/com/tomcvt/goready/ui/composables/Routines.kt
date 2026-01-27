package com.tomcvt.goready.ui.composables

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.tomcvt.goready.data.AlarmEntity
import com.tomcvt.goready.data.RoutineEntity
import com.tomcvt.goready.test.launchAlarmNow
import com.tomcvt.goready.ui.imagevectors.IconBell
import com.tomcvt.goready.util.hasExactlyOneGrapheme
import com.tomcvt.goready.util.isExactlyOneEmoji
import com.tomcvt.goready.viewmodel.AlarmViewModel
import com.tomcvt.goready.viewmodel.RoutinesViewModel
import java.time.DayOfWeek
import java.util.Locale


@Composable
fun RoutineListRoute(
    viewModel: RoutinesViewModel,
    rootController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val routineList by viewModel.routinesStateFlow.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val onAddRoutineClick = {
        //For now just adding
        viewModel.addRoutineInEditor(0)
    }
    val onDeleteClick: (RoutineEntity) -> Unit = { routine: RoutineEntity -> viewModel.deleteRoutine(routine) }


    val onCardClick: (RoutineEntity) -> Unit = {
        viewModel.selectRoutine(it.id)
        viewModel.openRoutineDetails()
    }
    Box (modifier = Modifier.fillMaxSize()) {
        RoutinesList(
            routineList = routineList,
            onAddClick = onAddRoutineClick,
            onDeleteClick = onDeleteClick,
            onCardClick = onCardClick,
            modifier = modifier
        )
        if(uiState.isRoutineDetailsOpen) {
            RoutineDetailsScreen(
                viewModel = viewModel,
                //navController = navController,
                modifier = modifier
            )
        }
        if(uiState.isRoutineEditorOpen) {
            RoutineEditor(
                viewModel = viewModel,
                //navController = navController,
                modifier = modifier
            )
        }

        if (uiState.successMessage != null) {
            //show succes modal
            StandardModal(
                onDismiss = { viewModel.clearSuccessMessage() },
                onConfirm = { viewModel.clearSuccessMessage() }
            ) {
                Text(text = uiState.successMessage?: "ERROR")
            }
            Log.d("RoutineListRoute", "Success message: ${uiState.successMessage}")

        }
    }

}

@Composable
fun RoutinesList(
    routineList: List<RoutineEntity>,
    onAddClick: () -> Unit,
    onDeleteClick: (RoutineEntity) -> Unit,
    onCardClick: (RoutineEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = modifier.fillMaxSize()) {
            Text(
                text = "Routines",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp)
            )
            LazyColumn {
                items(routineList.size) { index ->
                    val routine = routineList[index]
                    RoutineCard(
                        name = routine.name,
                        description = routine.description,
                        icon = routine.icon,
                        onDelete = { onDeleteClick(routine) },
                        onCardClick = { onCardClick(routine) },
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
        //here the button to add alarm
        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add Alarm",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun RoutineCard(
    name: String,
    description: String,
    icon: String,
    onDelete: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, end = 8.dp) // Add padding so the button doesn't look cut off
                .clickable { onCardClick() },
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = name, style = MaterialTheme.typography.headlineSmall)
                Text(text = icon, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // The Delete Button
        SimpleDeleteButton(
            onDelete = onDelete,
            modifier = Modifier
                .align(Alignment.TopEnd)
        )

    }
}

@Composable
fun RoutineEditor(
    viewModel: RoutinesViewModel,
    //navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val rEditorState by viewModel.routineEditorState.collectAsState()

    Box (
        modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.5f))
        .clickable { viewModel.closeRoutineEditor() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Edit routine", style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center)
            Card(
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row (horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    TextField(
                        value = rEditorState.name,
                        onValueChange = { viewModel.setRoutineName(it) },
                        modifier = Modifier.weight(1f)
                    )
                    //Spacer(modifier = Modifier.size(8.dp))
                    TextField(
                        value = rEditorState.icon,
                        onValueChange = {
                            if (isExactlyOneEmoji(it) || it.isEmpty()) viewModel.setRoutineIcon(it) },
                        placeholder = { Text("\uD83D\uDC4D") },
                        modifier = Modifier.width(50.dp)
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                TextField(
                    value = rEditorState.description,
                    onValueChange = { viewModel.setRoutineDescription(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rEditorState.steps.size) { index ->
                    val step = rEditorState.steps[index]
                    Card(
                        elevation = CardDefaults.cardElevation(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text(step.first.name)
                        Text(step.first.icon)
                        Text(step.second.toString())
                    }
                }
            }
            Button(
                onClick = { viewModel.openStepEditor() },
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Add step")
            }
            FloatingActionButton(
                onClick = { viewModel.saveRoutine()
                           viewModel.closeRoutineEditor()
                            viewModel.clearRoutineEditor() },
                modifier = Modifier.padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Text("Save")
            }
        }
        if (uiState.isStepEditorOpen) {
            StepEditor(
                viewModel = viewModel,
                //navController = navController,
                modifier = modifier
            )
        }
    }
}

@Composable
fun StepEditor(
    viewModel: RoutinesViewModel,
    //navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val stepEditorState by viewModel.stepEditorState.collectAsState()

    Box (
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { viewModel.closeStepEditor() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Add step", style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Card(
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    TextField(
                        value = stepEditorState.name,
                        onValueChange = { viewModel.setStepName(it) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    TextField(
                        value = stepEditorState.icon,
                        onValueChange = {
                            if (isExactlyOneEmoji(it) || it.isEmpty()) viewModel.setStepIcon(it)
                        },
                        placeholder = { Text("\uD83D\uDC4D") },
                        modifier = Modifier.width(50.dp)
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                TextField(
                    value = stepEditorState.description,
                    onValueChange = { viewModel.setStepDescription(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text("step type")
            FloatingActionButton(
                onClick = { viewModel.saveStepDefinitionAndAdd()
                          viewModel.closeStepEditor() },
                modifier = Modifier.padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
fun RoutineDetailsScreen(
    viewModel: RoutinesViewModel,
    //navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val routineSteps by viewModel.selectedRoutineSteps.collectAsState()
    val routineEntity by viewModel.selectedRoutineEntity.collectAsState()

    Box (
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { viewModel.closeRoutineDetails() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = routineEntity?.name ?: "NULL",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = routineEntity?.icon ?: "N",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(50.dp)
                        )
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = routineEntity?.description ?: "NULL",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(routineSteps.size) { index ->
                        val step = routineSteps[index]
                        Card(
                            elevation = CardDefaults.cardElevation(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(16.dp)
                            ) {
                                Text(
                                    step.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    step.icon,
                                    modifier = Modifier.width(50.dp)
                                )
                                Text(
                                    step.length.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.width(50.dp)
                                )
                            }
                        }
                    }
                }
                Button(
                    onClick = { viewModel.openRoutineEditorWithSelectedRoutine() },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("EDIT")
                } /*
            FloatingActionButton(
                onClick = { viewModel.saveRoutine()
                    viewModel.closeRoutineEditor()
                    viewModel.clearRoutineEditor() },
                modifier = Modifier.padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Text("Save")
            }
            */
            }
        }
    }
}
