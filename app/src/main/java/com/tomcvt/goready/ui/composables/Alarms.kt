package com.tomcvt.goready.ui.composables

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.layout.layoutId
import com.tomcvt.goready.ui.imagevectors.IconBell


@Composable
fun AlarmListRoute(
    viewModel: AlarmViewModel,
    navController: NavHostController,
    rootController: NavHostController,
    modifier: Modifier = Modifier
) {
    val alarmList by viewModel.alarmsStateFlow.collectAsState()
    val onAddAlarmClick = { }
    val onDeleteClick: (AlarmEntity) -> Unit = { alarm: AlarmEntity -> viewModel.deleteAlarm(alarm) }
    val onAlarmSwitchChange: (AlarmEntity, Boolean) -> Unit = {
        alarm: AlarmEntity, enabled: Boolean -> viewModel.toggleAlarm(alarm, enabled)
    }

    AlarmList(
        alarmList = alarmList,
        onAddClick = onAddAlarmClick,
        onDeleteClick = onDeleteClick,
        onAlarmSwitchChange = onAlarmSwitchChange,
        modifier = modifier
    )

}

@Composable
fun AlarmList(
    alarmList: List<AlarmEntity>,
    onAddClick: () -> Unit,
    onDeleteClick: (AlarmEntity) -> Unit,
    onAlarmSwitchChange: (AlarmEntity, Boolean) -> Unit,
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
                        onDelete = { onDeleteClick(alarm) },
                        enabled = alarm.isEnabled,
                        onToggleEnabled = {onAlarmSwitchChange(alarm, it)},
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
              enabled: Boolean,
              onDelete: () -> Unit,
              onToggleEnabled: (Boolean) -> Unit,
              repeatDays: Set<DayOfWeek>,
              modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, end = 8.dp), // Add padding so the button doesn't look cut off
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = alarmName, style = MaterialTheme.typography.headlineSmall)
                Text(text = alarmTime, style = MaterialTheme.typography.bodyMedium)
                Box(modifier = Modifier) {
                    DaysRow(repeatDays = repeatDays, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        // The Delete Button
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .align(Alignment.TopEnd) // Positions it at the top right
                .offset(-(10.dp), (10.dp))
                .padding(4.dp)
                .background(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp) // Square rounded
                )
                .layoutId("delete_button")
                .size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete Alarm",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp)
            )
        }

        Switch(
            checked = enabled,
            onCheckedChange = onToggleEnabled,
            thumbContent = if (enabled) {
                {
                    Icon(
                        imageVector = IconBell,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                }
            } else {
                null
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset((-24).dp, (-10).dp)
                //.padding(4.dp)
                .layoutId("switch")
                .size(32.dp)

        )
    }
    // My old approach
    /*
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

     */
}