package com.tomcvt.goready.ui.composables

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.tomcvt.goready.data.AlarmEntity
import com.tomcvt.goready.data.RoutineEntity
import com.tomcvt.goready.test.launchAlarmNow
import com.tomcvt.goready.ui.imagevectors.IconBell
import com.tomcvt.goready.viewmodel.AlarmViewModel
import com.tomcvt.goready.viewmodel.RoutinesViewModel
import java.time.DayOfWeek
import java.util.Locale


@Composable
fun RoutineListRoute(
    viewModel: RoutinesViewModel,
    navController: NavHostController,
    rootController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val routineList by viewModel.routinesStateFlow.collectAsState()
    val onAddRoutineClick = {
        //TODO open new routine popup
    }
    val onDeleteClick: (RoutineEntity) -> Unit = { routine: RoutineEntity -> viewModel.deleteRoutine(routine) }


    val onCardClick: (AlarmEntity) -> Unit = {
        //TODO open routine details
            //alarm: AlarmEntity -> rootController.navigate("edit_alarm/${alarm.id}")
    }

    RoutinesList(
        routineList = routineList,
        onAddClick = onAddRoutineClick,
        onDeleteClick = onDeleteClick,
        onCardClick = onCardClick,
        modifier = modifier
    )

}

@Composable
fun RoutinesList(
    routineList: List<RoutineEntity>,
    onAddClick: () -> Unit,
    onDeleteClick: (RoutineEntity) -> Unit,
    onCardClick: (AlarmEntity) -> Unit,
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
                items(routineList.size) { index ->
                    val routine = routineList[index]
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
                        onCardClick = { onCardClick(alarm) },
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
fun RoutineCard(alarmName: String,
              alarmTime: String,
              enabled: Boolean,
              onDelete: () -> Unit,
              onToggleEnabled: (Boolean) -> Unit,
              repeatDays: Set<DayOfWeek>,
              onCardClick: () -> Unit,
              modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, end = 8.dp) // Add padding so the button doesn't look cut off
                .clickable { onCardClick() },
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
        SimpleDeleteButton(
            onDelete = onDelete,
            modifier = Modifier
                .align(Alignment.TopEnd)
        )

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
}