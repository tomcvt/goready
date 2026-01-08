package com.tomcvt.goready.ui.composables

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.tomcvt.goready.data.AlarmEntity
import com.tomcvt.goready.preview.PreviewAlarms
import com.tomcvt.goready.preview.PreviewAlarms2
import com.tomcvt.goready.viewmodel.AlarmViewModel
import java.time.DayOfWeek
import java.util.Locale
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add


@Composable
fun AlarmListRoute(
    viewModel: AlarmViewModel,
    navController: NavHostController,
    rootController: NavHostController,
    modifier: Modifier = Modifier
) {
    val alarmList by viewModel.alarmsStateFlow.collectAsState()
    val onAddAlarmClick = { rootController.navigate("ADD_ALARM") }
    val onDeleteClick: (AlarmEntity) -> Unit = { alarm: AlarmEntity -> viewModel.deleteAlarm(alarm) }

    AlarmList(
        alarmList = alarmList,
        onAddClick = onAddAlarmClick,
        onDeleteClick = onDeleteClick,
        modifier = modifier
    )
}

@Composable
fun AlarmList(
    alarmList: List<AlarmEntity>,
    onAddClick: () -> Unit,
    onDeleteClick: (AlarmEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = modifier.fillMaxSize()) {
            Text(
                text = "Alarms",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp)
            )
            LazyColumn {
                items(alarmList.size) { alarm ->
                    val alarm = alarmList[alarm]
                    AlarmCard(
                        alarmName = alarm.label ?: "Alarm",
                        alarmTime = String.format(
                            Locale.getDefault(),
                            "%02d:%02d",
                            alarm.hour,
                            alarm.minute
                        ),
                        onDelete = {},
                        onToggleEnabled = {},
                        repeatDays = alarm.repeatDays,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
        //here the button to add alarm
        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
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
fun AlarmCard(alarmName: String,
              alarmTime: String,
              onDelete: () -> Unit,
              onToggleEnabled: (Boolean) -> Unit,
              repeatDays: Set<DayOfWeek>,
              modifier: Modifier = Modifier) {
    // Placeholder for the AlarmCard composable
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            //Text(text = alarmName, style = MaterialTheme.typography.h6) //TODO : typography
            //Text(text = alarmTime, style = MaterialTheme.typography.body2)
            Text(text = alarmName, style = MaterialTheme.typography.headlineSmall)
            Text(text = alarmTime, style = MaterialTheme.typography.bodyMedium)
            DaysRow(repeatDays = repeatDays, modifier = Modifier.padding(top = 8.dp))
        }
    }
}