package com.tomcvt.goready

import android.app.TimePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.tomcvt.goready.ui.theme.GoReadyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GoReadyTheme {
                GoReadyApp()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun GoReadyApp() {
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
                AlarmList(Modifier.padding(innerPadding))
            }
            if (currentDestination == AppDestinations.PROFILE) {
                Greeting(
                    name = "Profile",
                    modifier = Modifier.padding(innerPadding)
                )
            }
            if (currentDestination == AppDestinations.ADD_ALARM) {
                AddAlarmView(Modifier.padding(innerPadding))
            }
        }
    }
}

enum class DayOfWeek { MON, TUE, WED, THU, FRI, SAT, SUN }
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
fun AlarmList(modifier: Modifier = Modifier) {
    Text(text = "Alarm List Placeholder")
}

@Composable
fun AddAlarmView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var selectedDays by remember {
        mutableStateOf(setOf<DayOfWeek>())
    }
    var selectedHour: Int = 8
    var selectedMinute: Int = 30
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Selected time: %02d:%02d".format(selectedHour, selectedMinute),
            modifier = Modifier
                .absoluteOffset(y = 50.dp)
                .padding(16.dp)
                .clickable {
                    picker.show()
                }
        )

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

        Button(onClick = {}) {
            Text("Save Alarm")
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
fun AddAlarmViewPreview() {
    GoReadyTheme {
        AddAlarmView()
    }
}