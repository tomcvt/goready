package com.tomcvt.goready.ui.composables

import android.content.Intent
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.tomcvt.goready.activities.RoutineFlowActivity
import com.tomcvt.goready.constants.ACTION_RF_UI_LAUNCHER
import com.tomcvt.goready.constants.EXTRA_ROUTINE_ID
import com.tomcvt.goready.constants.StepType
import com.tomcvt.goready.constants.StepTypeSelector
import com.tomcvt.goready.data.RoutineEntity
import com.tomcvt.goready.data.StepDefinitionEntity
import com.tomcvt.goready.data.StepWithDefinition
import com.tomcvt.goready.util.isExactlyOneEmoji
import com.tomcvt.goready.viewmodel.RoutinesViewModel
import com.tomcvt.goready.viewmodel.UiEvent


@Composable
fun RoutineListRoute(
    viewModel: RoutinesViewModel,
    rootController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val routineList by viewModel.routinesStateFlow.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect {
            when (it) {
                is UiEvent.OpenRoutineLauncher ->  {
                    val intent = Intent(
                        context,
                        RoutineFlowActivity::class.java
                    ).apply {
                        putExtra(EXTRA_ROUTINE_ID, it.routineId)
                        setAction(ACTION_RF_UI_LAUNCHER)
                    }
                    context.startActivity(intent)
                }
            }
        }
    }

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
            onStartClick = {
                viewModel.openRoutineLauncher(it.id)
            },
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

        if (uiState.errorMessage != null) {
            StandardModal(
                onDismiss = { viewModel.clearErrorMessage() },
                onConfirm = { viewModel.clearErrorMessage() }
            ) {
                Text(text = uiState.errorMessage?: "ERROR")
            }
        }

    }
}

@Composable
fun RoutinesList(
    routineList: List<RoutineEntity>,
    onAddClick: () -> Unit,
    onDeleteClick: (RoutineEntity) -> Unit,
    onCardClick: (RoutineEntity) -> Unit,
    onStartClick: (RoutineEntity) -> Unit,
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
                        onStartClick = { onStartClick(routine) },
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
    onStartClick: () -> Unit,
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
        Row (
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SimpleStartButton(
                onStart = onStartClick,
                modifier = Modifier,
                size = 32.dp,
                contentDescription = "Start Routine"
            )
            FlexDeleteButton(
                onDelete = onDelete,
                modifier = Modifier,
                size = 32.dp,
                contentDescription = "Delete Routine"
            )
        }

        // The Start Button


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

    BackHandler(
        enabled = true,
        onBack = { viewModel.closeRoutineEditor() }
    )

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
            RoutineEditorDetailsCard(viewModel)
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rEditorState.steps.size) { index ->
                    val step = rEditorState.steps[index]
                    RoutineEditorStepCard(
                        viewModel, index, step,
                        modifier = Modifier.fillMaxWidth(),
                        onEdit = {
                            viewModel.openStepEditorWithStep(step.first, index)
                        },
                        onDelete = {
                            viewModel.removeStepFromRoutineEditor(index)
                        }
                    )
                }
            }
            Button(
                onClick = { viewModel.openStepAdder() },
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Add step")
            }
            FloatingActionButton(
                onClick = { viewModel.saveRoutine() },
                modifier = Modifier.padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Text("Save")
            }
        }
        if (uiState.stepModalNumber != null) {
            StepTimeModal(
                initialTime = rEditorState.steps[uiState.stepModalNumber!!].second,
                onDismiss = { viewModel.clearStepModalNumber() },
                onProvidedValue = { viewModel.updateTimeForStepInEditor(it, uiState.stepModalNumber!!) },
                list = STEP_TIMES_LIST
            )
        }
        if (uiState.isStepAdderOpen) {
            StepSelector(
                viewModel = viewModel,
                //navController = navController,
                modifier = modifier
            )
        }
        if (uiState.isStepEditorOpen) {
            StepEditorWrapper(
                viewModel = viewModel,
                //navController = navController,
                modifier = modifier
            )
        }
    }
}

@Composable
fun StepEditorWrapper(
    viewModel: RoutinesViewModel,
    //navController: NavHostController,
    modifier: Modifier = Modifier
) {
    StepEditorBox(
        viewModel = viewModel,
        //navController = navController,
        onDismiss = { viewModel.closeStepEditor() },
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { viewModel.closeStepEditor() }
    )
}

@Composable
fun CategoryPill(
    category: StepType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pillContent: @Composable (label: String) -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        border = if (selected) {
            BorderStroke(2.dp, Color.Black.copy(alpha = 0.5f))
        } else null,
        colors = ButtonDefaults.buttonColors(
            containerColor = category.color,
            contentColor = category.textColor
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        pillContent(category.label)
    }
}

@Composable
fun RoutineEditorDetailsCard(
    viewModel: RoutinesViewModel,
    modifier: Modifier = Modifier
) {
    val rEditorState by viewModel.routineEditorState.collectAsState()

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
}

@Composable
fun RoutineEditorStepCard(
    viewModel: RoutinesViewModel,
    index: Int,
    step: Pair<StepDefinitionEntity, Int>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {expanded = false},
                onLongClick = { expanded = true }
            )
    ) {
        Card(
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text(
                    text = step.first.name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = step.first.icon,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(50.dp)
                )
                Text(
                    text = step.second.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.width(50.dp)
                        .clickable { viewModel.setStepModalNumber(index) }
                )
            }
        }
        StepCardOverlay(
            expanded = expanded,
            updatable = step.first.updatable,
            onDismiss = { expanded = false },
            onDelete = { onDelete(); expanded = false },
            onEdit = { onEdit(); expanded = false },
            modifier = Modifier.matchParentSize()
        )
    }
}

@Composable
fun StepCardOverlay(
    expanded: Boolean,
    updatable: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = expanded,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it }),
        modifier = modifier
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (updatable) {
                StaticEditButton(onEdit)
            }
            StaticDeleteButton(onDelete)
        }
    }
}

@Composable
fun StaticEditButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.padding(16.dp)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        Icon(
            Icons.Default.Edit,
            contentDescription = "Edit",
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun StaticDeleteButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.padding(16.dp)
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        Icon(
            Icons.Default.Delete,
            contentDescription = "Delete",
            tint = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

val STEP_TIMES_LIST = (5..90 step 5).toList()

@Composable
fun StepSelector(
    viewModel: RoutinesViewModel,
    //navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val categories = StepType.getCategories()
    val selectedSelector by viewModel.stepTypeSelector.collectAsState()
    val selectedCategory by viewModel.selectedStepType.collectAsState()
    val selectedSteps by viewModel.selectedStepsByType.collectAsState()
    val userStepDefs by viewModel.userStepDefs.collectAsState()


    BackHandler(
        enabled = true,
        onBack = { viewModel.closeStepAdder() }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { viewModel.closeStepAdder() },
        contentAlignment = Alignment.Center
    ) {
        Column (
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Button(
                        onClick = { viewModel.setStepTypeSelector(StepTypeSelector.Add) }
                    ) {
                        Text("Add")
                    }
                }
                item {
                    Button(
                        onClick = { viewModel.setStepTypeSelector(StepTypeSelector.User) }
                    ) {
                        Text("Created")
                    }
                }
            }
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories.size)  {
                    CategoryPill(
                        category = categories[it],
                        selected = selectedCategory == categories[it],
                        onClick = { viewModel.setStepTypeSelector(
                            StepTypeSelector.SelectedType(categories[it])
                        ) }
                    ) { label ->
                        Text(label)
                    }
                }
            }
            when (val selected = selectedSelector) {
                is StepTypeSelector.SelectedType -> {
                    StepSelectorByType(
                        viewModel,
                        selectedSteps,
                        modifier = Modifier.weight(0.5f)
                    )
                }
                is StepTypeSelector.Add -> {
                    StepEditorBox(
                        viewModel,
                        onDismiss = { viewModel.closeStepAdder() },
                        modifier = Modifier.weight(0.5f)
                    )
                }
                is StepTypeSelector.User -> {
                    StepSelectorByType(
                        viewModel,
                        userStepDefs,
                        modifier = Modifier.weight(0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun StepSelectorByType(
    viewModel: RoutinesViewModel,
    selectedSteps: List<StepDefinitionEntity>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            //.fillMaxSize()
            //.background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = { viewModel.closeStepEditor() }),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(selectedSteps.size) { index ->
                val step = selectedSteps[index]
                StepDefRowCardClickable(
                    step,
                    onClick = { viewModel.addStepDefToRoutineEditor(step) }
                )
            }
        }
    }
}


@Composable
fun StepEditorBox(
    viewModel: RoutinesViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stepEditorState by viewModel.stepEditorState.collectAsState()
    var showStepTypeModal by remember { mutableStateOf(false) }

    Box (
        modifier = modifier
            //.fillMaxSize()
            //.background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                Button(
                    onClick = { showStepTypeModal = true },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Select step type")
                }
                Button(
                    onClick = { viewModel.saveStepDefinition() },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Save")
                }
            }
        }
        if (showStepTypeModal) {
            SelectItemFlowRowModal(
                text = "Select step type",
                onDismiss = { showStepTypeModal = false },
                onItemSelected = { viewModel.setStepType(it) },
                onConfirm = { showStepTypeModal = false },
                list = StepType.getCategories(),
                startingItem = stepEditorState.stepType
            ) { item, selected, onClick ->
                CategoryPill(
                    category = item,
                    selected = selected,
                    onClick = onClick
                ) { label ->
                    Text(label)
                }
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

    BackHandler(
        enabled = true,
        onBack = { viewModel.closeRoutineDetails() }
    )

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
                        onClick = { viewModel.openRoutineEditorWithSelectedRoutine() },
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("EDIT")
                    }
                }
                FlexCloseButton(
                    onClose = { viewModel.closeRoutineDetails() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                )
            }
        }
    }
}

@Composable
fun RoutineEntityDetails(
    routineEntity: RoutineEntity?,
    modifier: Modifier = Modifier,

) {
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
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = routineEntity?.icon ?: "N",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.width(50.dp)
            )
        }
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = routineEntity?.description ?: "NULL",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun StepDefRowCardClickable(
    step: StepDefinitionEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text(
                step.name,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            Text(
                step.icon,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
            )
        }
    }
}


@Composable
fun StepRowCard(
    step: StepWithDefinition
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
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
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            Text(
                step.icon,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
            )
            Text(
                step.length.toString(),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier
            )
        }
    }
}
