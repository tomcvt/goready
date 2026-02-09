package com.tomcvt.goready.ui.composables

import android.app.TimePickerDialog
import android.util.Log
import android.widget.ToggleButton
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.tomcvt.goready.LocalPremiumState
import com.tomcvt.goready.RootTab
import com.tomcvt.goready.constants.TaskType
import com.tomcvt.goready.constants.TaskTypeContext
import com.tomcvt.goready.domain.AlarmDraft
import com.tomcvt.goready.domain.SimpleAlarmDraft
import com.tomcvt.goready.viewmodel.AlarmViewModel
import com.tomcvt.goready.viewmodel.UiState
import org.checkerframework.framework.qual.Unused
import java.time.DayOfWeek


@Composable
fun AddAlarmRoute(viewModel: AlarmViewModel,
                  rootNavController: NavHostController?,
                  modifier: Modifier = Modifier,
                  alarmId: Long? = null
    ) {
    AddAlarmView(viewModel, rootNavController, modifier, null)
}

@Composable
fun AddAlarmView(viewModel: AlarmViewModel,
                 rootNavController: NavHostController?,
                 modifier: Modifier = Modifier,
                 alarmId: Long? = null
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(alarmId) {
        viewModel.initEditor(alarmId)
    }
    val state by viewModel.editorState.collectAsState()
    var showExit by remember {mutableStateOf(false)}

    Box (modifier = modifier) {
        AddAlarmContent(viewModel, rootNavController)

        uiState.successMessage?.let {
            AlarmAddedModal(
                it,
                taskData = state.taskData,
                onDismiss = { viewModel.clearSuccessMessage()
                    //TODO check if its logically ok
                                rootNavController?.popBackStack()
                                rootNavController?.navigate(RootTab.ALARMS.name) {
                                popUpTo(rootNavController.graph.startDestinationId) {
                                    saveState = false
                                }
                                launchSingleTop = true
                                restoreState = false
                            }
                            },
                hour = state.hour,
                minute = state.minute,
                days = state.repeatDays
            )
        }
        uiState.errorMessage?.let {
            StandardModal(
                onDismiss = { viewModel.clearErrorMessage() },
                onConfirm = { viewModel.clearErrorMessage() }
            ) {
                Text(it, textAlign = TextAlign.Center)
            }
        }
        uiState.inputErrorMessage?.let {
            StandardModal(
                onDismiss = { viewModel.clearInputErrorMessage() },
                onConfirm = { viewModel.clearInputErrorMessage() }
            ) {
                Text(it, textAlign = TextAlign.Center)
            }
        }

    }
    BackHandler(
        enabled = true,
        onBack = { showExit = true }
    )

    if (showExit) {
        StandardModal(
            onDismiss = { showExit = false },
            onConfirm = { rootNavController?.popBackStack() }
        ) {
            Text("Do you want to exit?")
        }
    }
}

@Composable
fun AddAlarmContent(
    viewModel: AlarmViewModel,
    rootNavController: NavHostController?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val state by viewModel.editorState.collectAsState()
    val rememberedData by viewModel.rememberedData.collectAsState()
    var snoozeModal by remember {mutableStateOf(false)}

    val picker = TimePickerDialog(
        context,
        { _, hour, minute ->
            viewModel.setTime(hour, minute)
        },
        8,
        30,
        true
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier.padding(24.dp),
            ) {
                Text(
                    "%02d:%02d".format(state.hour, state.minute),
                    modifier = Modifier
                        .padding(16.dp)
                        .clickable {
                            picker.show()
                        },
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row {
                DayOfWeek.values().forEach { day ->
                    FilterChip(
                        selected = day in state.repeatDays,
                        onClick = {
                            viewModel.toggleDay(day)
                        },
                        label = { Text(day.name.take(1)) }
                    )
                }
            }
            AlarmTypeSelector(
                value = state.taskType,
                options = TaskType.getList(),
                onTypeSelected = { viewModel.setTaskType(it) }
            )
            TaskDataInput(
                value = rememberedData[state.taskType.name] ?: "",
                taskType = state.taskType,
                onPremiumRequest = { TODO("implement premium request") },
                onTaskDataProvided = {
                    Log.d("AddAlarmView", "Task data provided: ${it}")
                    //if (!it.isBlank()){
                    viewModel.setTaskData(it)
                    //}
                }
            )
            SnoozeInfoRow(
                snoozeCount = state.snoozeCount,
                snoozeTime = state.snoozeTime,
                snoozeActive = state.snoozeActive,
                onClick = { snoozeModal = true },
                onSwitchChange = { viewModel.setSnoozeActive(it) }
            )
            state.routineId?.let {
                Text("Routine ID: $it")
                //SelectedRoutineRow
            }
            Button(onClick = { viewModel.openRoutineSelector() }) {
                //TODO implement viewmodel
                Text("Select Routine")
            }




            Button(onClick = { viewModel.save() }) {
                Text("Save Alarm")
            }
        }
        if (snoozeModal) {
            SnoozeInputModal(
                onDismiss = { snoozeModal = false },
                onConfirm = { snoozeModal = false },
                onInputChange = { snoozeCount, snoozeTime ->
                    Log.d("SnoozeInputModal", "Snooze count: $snoozeCount, snooze time: $snoozeTime")
                    viewModel.setSnoozeCount(snoozeCount)
                    viewModel.setSnoozeTime(snoozeTime)
                },
                startingCount = state.snoozeCount,
                startingTime = state.snoozeTime
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmTypeSelector(
    value: TaskType,
    options: List<TaskType>,
    onTypeSelected: (TaskType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    //var selectedOption by remember { mutableStateOf(selectedType.label) }
    val premiumState = LocalPremiumState.current

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        TextField(
            value = value.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Select an option") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                val shouldDim = !premiumState.isPremium && option.premium
                DropdownMenuItem(
                    text = { Text(option.name) },
                    onClick = {
                        onTypeSelected(option)  // call lambda
                        expanded = false
                    },
                    modifier = if (shouldDim) Modifier.alpha(0.5f) else Modifier
                )
            }
        }
    }
}

@Composable
fun TaskDataInput(
    value: String,
    taskType: TaskType,
    onPremiumRequest: () -> Unit,
    onTaskDataProvided: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val premiumState = LocalPremiumState.current

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if(!premiumState.isPremium && taskType.premium) {
            PremiumContentLabelBox(
                onClickListener = onPremiumRequest,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        } else {

            when (taskType) {
                TaskType.TIMER -> {}
                TaskType.COUNTDOWN -> {
                    NumbersInput(
                        value = value,
                        onValueChange = {
                            onTaskDataProvided(it)
                        },
                        onFocusLost = { onTaskDataProvided(it) },
                        placeholder = "   ",
                        modifier = Modifier.widthIn(min = 100.dp, max = 250.dp)
                    )
                }

                TaskType.TEXT -> {
                    TextInputCard(
                        value = value,
                        onTextChange = {
                            onTaskDataProvided(it)
                        },
                        onFocusLost = { onTaskDataProvided(it) },
                        placeholder = "Type here..."
                    )
                }

                TaskType.MATH -> {
                    MathTaskInput(
                        value = value,
                        onInputChange = {
                            onTaskDataProvided(it)
                        },
                        onFocusLost = { onTaskDataProvided(it) }
                    )
                }

                TaskType.TARGET -> {
                    //TODO implement inputs sizing by text size
                    NumbersInput(
                        value = value,
                        onValueChange = {
                            onTaskDataProvided(it)
                        },
                        onFocusLost = { onTaskDataProvided(it) },
                        placeholder = "   ",
                        modifier = Modifier.widthIn(min = 100.dp, max = 250.dp)
                    )
                }

                else -> {}
            }
        }
    }
}

@Deprecated("Not used")
fun parseData(taskType: TaskType, taskData: String) : String?  {
    when (taskType) {
        TaskType.TIMER -> {return null}
        TaskType.COUNTDOWN -> {
            if (taskData.isDigitsOnly() && taskData.toInt() > 0) {
                return taskData
            }
            return null
        }
        TaskType.TEXT -> {
            if (taskData.isNotBlank()) {
                return taskData
            }
            return null
        }
        TaskType.MATH -> {return taskData}
        else -> {return null}
    }
}

@Composable
fun SnoozeInfoRow(
    snoozeCount: Int,
    snoozeTime: Int,
    snoozeActive: Boolean,
    onClick: () -> Unit,
    onSwitchChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp).padding(end = 16.dp)
                .clickable { onClick() }
        ) {
            Text("Snooze %d %s".format(snoozeTime, if (snoozeTime == 1) "minute" else "minutes"))
            Switch(
                checked = snoozeActive,
                onCheckedChange = {
                    onSwitchChange(it)
                    //TODO viewmodel switch snooze
                },
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun PremiumContentLabelBox(
    onClickListener: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(Color.LightGray.copy(alpha = 0.5f))
            .clickable { onClickListener() }
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Premium feature",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
    }
}