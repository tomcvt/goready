package com.tomcvt.goready.ui.composables

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
    var uiState by viewModel.uiState.collectAsState()
    val onAddRoutineClick = {
        viewModel.startNew()
    }
    val onDeleteClick: (RoutineEntity) -> Unit = { routine: RoutineEntity -> viewModel.deleteRoutine(routine) }


    val onCardClick: (RoutineEntity) -> Unit = {
        //TODO open routine details
            //alarm: AlarmEntity -> rootController.navigate("edit_alarm/${alarm.id}")
    }
    Box (modifier = Modifier.fillMaxSize()) {
        RoutinesList(
            routineList = routineList,
            onAddClick = onAddRoutineClick,
            onDeleteClick = onDeleteClick,
            onCardClick = onCardClick,
            modifier = modifier
        )
        if(uiState.isRoutineEditorOpen) {
            //show routine editor
        }



        if (uiState.successMessage != null) {
            //show succes modal
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
                text = "Alarms",
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
    viewModel: AlarmViewModel,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current



)