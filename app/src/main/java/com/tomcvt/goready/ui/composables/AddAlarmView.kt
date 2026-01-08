package com.tomcvt.goready.ui.composables

import android.app.TimePickerDialog
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tomcvt.goready.domain.SimpleAlarmDraft
import com.tomcvt.goready.viewmodel.AlarmViewModel
import com.tomcvt.goready.viewmodel.UiState
import java.time.DayOfWeek


@Composable
fun AddAlarmRoute(viewModel: AlarmViewModel) {
    AddAlarmView(viewModel)
}

@Composable
fun AddAlarmView(viewModel: AlarmViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current


    val uiState by viewModel.uiState.collectAsState()

    var selectedDays by remember {
        mutableStateOf(setOf<DayOfWeek>())
    }
    var showModal by remember {mutableStateOf(false)}
    var selectedHour by remember {mutableIntStateOf(8)}
    var selectedMinute by remember {mutableIntStateOf(30)}
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

            Button(onClick = {showModal = true
                val newDraftAlarm = SimpleAlarmDraft(
                    hour = selectedHour,
                    minute = selectedMinute,
                    repeatDays = selectedDays
                )
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
                    onDismiss = { showModal = false },
                    hour = selectedHour,
                    minute = selectedMinute,
                    days = selectedDays
                )
            }
        }

    }
}