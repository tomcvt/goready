package com.tomcvt.goready

import android.app.TimePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.tomcvt.goready.domain.SimpleAlarmDraft
import com.tomcvt.goready.ui.composables.AlarmList
import com.tomcvt.goready.ui.theme.GoReadyTheme
import com.tomcvt.goready.viewmodel.AlarmViewModel
import com.tomcvt.goready.viewmodel.AlarmViewModelProvider
import com.tomcvt.goready.viewmodel.UiState
import java.time.DayOfWeek

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val alarmViewModel: AlarmViewModel = AlarmViewModelProvider.provideAlarmViewModel(this)
            GoReadyTheme {
                GoReadyApp(alarmViewModel)
            }
        }
    }
}

//@PreviewScreenSizes
@Composable
fun GoReadyApp(viewModel: AlarmViewModel) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            if (currentDestination == AppDestinations.HOME) {
                Greeting(
                    name = "Home",
                    modifier = Modifier.padding(innerPadding)
                )
            }
            if (currentDestination == AppDestinations.FAVORITES) {
                AlarmList(viewModel, Modifier.padding(innerPadding))
            }
            if (currentDestination == AppDestinations.PROFILE) {
                Greeting(
                    name = "Profile",
                    modifier = Modifier.padding(innerPadding)
                )
            }
            if (currentDestination == AppDestinations.ADD_ALARM) {
                AddAlarmView(viewModel, Modifier.padding(innerPadding))
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Home", Icons.Default.Home),
    FAVORITES("Favorites", Icons.Default.Favorite),
    PROFILE("Profile", Icons.Default.AccountBox),
    ADD_ALARM("Add Alarm", Icons.Default.AddCircle),
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

        /*
        if (showModal) {
            AlarmAddedModal(
                onDismiss = { showModal = false },
                hour = selectedHour,
                minute = selectedMinute,
                days = selectedDays
            )
        }
         */


    }
}

@Composable
fun AlarmAddedModal(text: String?, modifier: Modifier = Modifier, onDismiss: () -> Unit , hour : Int, minute: Int, days: Set<DayOfWeek>) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.clickable(enabled = false) {},
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Text(text = text ?: "No viewModel action message")
                Text(
                    text = "Alarm set for %02d:%02d on %s".format(
                        hour,
                        minute,
                        if (days.isEmpty()) "no days" else days.joinToString { it.name.take(3) }
                    )
                )
                Button(onClick = onDismiss) { Text("OK") }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GoReadyTheme {
        Greeting("Android")
    }
}

@Preview(showBackground = true)
@Composable
fun AlarmListPreview() {
    GoReadyTheme {
        AlarmList(Modifier)
    }
}

@Preview(showBackground = true)
@Composable
fun AddAlarmViewPreview() {
    GoReadyTheme {
        //AddAlarmView(dummyViewModel)
    }
}