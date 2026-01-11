package com.tomcvt.goready.ui.composables

import android.app.TimePickerDialog
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.tomcvt.goready.constants.TaskType
import com.tomcvt.goready.constants.TaskTypeContext
import com.tomcvt.goready.domain.SimpleAlarmDraft
import com.tomcvt.goready.ui.navigation.RootTab
import com.tomcvt.goready.viewmodel.AlarmViewModel
import com.tomcvt.goready.viewmodel.UiState
import java.time.DayOfWeek


@Composable
fun AddAlarmRoute(viewModel: AlarmViewModel,
                  rootNavController: NavHostController,
                  modifier: Modifier = Modifier
    ) {
    AddAlarmView(viewModel, rootNavController, modifier)
}

@Composable
fun AddAlarmView(viewModel: AlarmViewModel,
                 rootNavController: NavHostController,
                 modifier: Modifier = Modifier) {
    val context = LocalContext.current


    val uiState by viewModel.uiState.collectAsState()

    var selectedDays by remember {
        mutableStateOf(setOf<DayOfWeek>())
    }
    var showModal by remember {mutableStateOf(false)}
    var selectedHour by remember {mutableIntStateOf(8)}
    var selectedMinute by remember {mutableIntStateOf(30)}
    var selectedType by remember {mutableStateOf(TaskType.NONE)}

    // Temporary variables to hold selected time from the picker
    // These will be updated when the user selects a time
    // and then saved to the state variables above when confirmed
    val picker = TimePickerDialog(
        context,
        { _, hour, minute ->
            selectedHour = hour
            selectedMinute = minute
        },
        8,
        30,
        true
    )
    Box (modifier = modifier) {
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E0E0))
            ) {
                Text(
                    "%02d:%02d".format(selectedHour, selectedMinute),
                    modifier = Modifier
                        .padding(16.dp)
                        .clickable {
                            picker.show()
                        }
                )
            }
            Row {
                DayOfWeek.values().forEach { day ->
                    FilterChip(
                        selected = day in selectedDays,
                        onClick = {
                            selectedDays =
                                if (day in selectedDays)
                                    selectedDays - day
                                else
                                    selectedDays + day
                        },
                        label = { Text(day.name.take(1)) }
                    )
                }
            }
            AlarmTypeSelector(
                options = TaskType.getList(),
                onTypeSelected = { selectedType = it }
            )


            Button(onClick = {showModal = true
                val newDraftAlarm = SimpleAlarmDraft(
                    hour = selectedHour,
                    minute = selectedMinute,
                    repeatDays = selectedDays
                )
                Log.d("AddAlarmView", "Type: ${selectedType.name}")
                viewModel.saveSimpleAlarm(newDraftAlarm)
            }) {
                Text("Save Alarm")
            }
        }
        val message = when (uiState) {
            is UiState.Success -> (uiState as UiState.Success).message
            is UiState.Error -> (uiState as UiState.Error).message
            else -> null
        }

        if (showModal) {
            message?.let {
                AlarmAddedModal(
                    it,
                    onDismiss = { showModal = false
                                    rootNavController.navigate(RootTab.ALARMS.name) {
                                        // Clear the "Add Alarm" screen from the history
                                        popUpTo(RootTab.ADD_ALARM.name) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                    hour = selectedHour,
                    minute = selectedMinute,
                    days = selectedDays
                )
            }
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmTypeSelector(
    options: List<TaskType>,
    onTypeSelected: (TaskType) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedType by remember { mutableStateOf(TaskType.NONE) }
    var expanded by remember { mutableStateOf(false) }
    //var selectedOption by remember { mutableStateOf(selectedType.label) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        TextField(
            value = selectedType.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Select an option") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name) },
                    onClick = {
                        selectedType = option
                        onTypeSelected(option)  // call lambda
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun TaskDataInput(
    taskType: TaskType,
    onTaskDataProvided: (TaskTypeContext) -> Unit
) {
    var taskData by remember { mutableStateOf(TaskTypeContext(taskType)) }
    when (taskType) {
        TaskType.TIMER -> {}
        TaskType.COUNTDOWN -> {}
        TaskType.TEXT -> {
            TextInputCard(
                onTextChange = { taskData.simpleData = it },
                onFocusLost = { onTaskDataProvided(taskData) },
                placeholder = "Type here..."
            )
        }
        TaskType.MATH -> {}
        else -> {}
    }
}

@Composable
fun TextInputCard(
    onTextChange: (String) -> Unit,
    onFocusLost: (String) -> Unit,
    placeholder: String = "Type here..."
) {
    var internalText by remember { mutableStateOf("") }

    OutlinedTextField(
        value = internalText,
        onValueChange = { internalText = it; onTextChange(it) },
        placeholder = { Text(placeholder) },
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { state ->
                if (!state.isFocused) {
                    // Call parent lambda when focus leaves
                    onFocusLost(internalText)
                }
            },
        singleLine = false,        // allows multiple lines
        maxLines = 10              // or any number you want
    )
}
