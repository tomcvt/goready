package com.tomcvt.goready.ui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.tomcvt.goready.data.AlarmEntity
import com.tomcvt.goready.preview.PreviewAlarms
import com.tomcvt.goready.preview.PreviewAlarms2
import com.tomcvt.goready.viewmodel.AlarmViewModel
import java.time.DayOfWeek
import java.util.Locale


@Composable
fun AlarmList(
    viewModel: AlarmViewModel,
    modifier: Modifier = Modifier
) {
    val alarmList by viewModel.alarmsStateFlow.collectAsState()
    AlarmList(
        alarmList = alarmList,
        modifier = modifier
    )
}

@Composable
fun AlarmList(
    alarmList: List<AlarmEntity>,
    modifier: Modifier = Modifier
) {
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
                    alarmTime = String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute),
                    onDelete = {},
                    onToggleEnabled = {},
                    repeatDays = alarm.repeatDays,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}



@Composable
fun AlarmList(modifier: Modifier = Modifier) {
    val alarmList = PreviewAlarms2().alarmList
    AlarmList(
        alarmList = alarmList,
        modifier = modifier
    )
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