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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tomcvt.goready.data.AlarmDatabase
import com.tomcvt.goready.domain.SimpleAlarmDraft
import com.tomcvt.goready.manager.AlarmManager
import com.tomcvt.goready.manager.SystemAlarmScheduler
import com.tomcvt.goready.preview.PreviewAlarms2
import com.tomcvt.goready.repository.AlarmRepository
import com.tomcvt.goready.ui.composables.AddAlarmView
import com.tomcvt.goready.ui.composables.AlarmAddedModal
import com.tomcvt.goready.ui.composables.AlarmList
import com.tomcvt.goready.ui.composables.AlarmsNavHost
import com.tomcvt.goready.ui.composables.HomeScreen
import com.tomcvt.goready.ui.composables.SettingsView
import com.tomcvt.goready.ui.navigation.LocalRootNavigator
import com.tomcvt.goready.ui.navigation.RootContent
import com.tomcvt.goready.ui.navigation.RootNavigatorImpl
import com.tomcvt.goready.ui.navigation.RootTab
import com.tomcvt.goready.ui.theme.GoReadyTheme
import com.tomcvt.goready.viewmodel.AlarmViewModel
import com.tomcvt.goready.viewmodel.AlarmViewModelFactory
import com.tomcvt.goready.viewmodel.AlarmViewModelProvider
import com.tomcvt.goready.viewmodel.UiState
import java.time.DayOfWeek

class MainActivity : ComponentActivity() {
    lateinit var alarmManager: AlarmManager
        private set

    lateinit var alarmViewModelFactory: AlarmViewModelFactory
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AlarmDatabase.getDatabase(this)
        val repository = AlarmRepository(db.alarmDao())
        alarmManager = AlarmManager(repository, SystemAlarmScheduler(this))
        alarmViewModelFactory = AlarmViewModelFactory(alarmManager)

        enableEdgeToEdge()
        setContent {
            //val alarmViewModel: AlarmViewModel = AlarmViewModelProvider.provideAlarmViewModel(this)
            val rootNavigator = remember { RootNavigatorImpl(start = RootTab.HOME) }
            CompositionLocalProvider(
                LocalRootNavigator provides rootNavigator
            ) {
                GoReadyTheme {
                    GoReadyApp(alarmViewModelFactory)
                }
            }

        }
    }
}

//@PreviewScreenSizes
@Composable
fun GoReadyApp(alarmViewModelFactory: AlarmViewModelFactory) {
    var currentDestination by rememberSaveable { mutableStateOf(RootTab.HOME) }

    val localRootNavigator = LocalRootNavigator.current

    val rootNavController = rememberNavController()


    NavigationSuiteScaffold(
        navigationSuiteItems = {
            RootTab.entries.forEach { tab ->
                item(
                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                    label = { Text(tab.label) },
                    selected = false, // NavController decides this
                    onClick = {
                        rootNavController.navigate(tab.name) {
                            popUpTo(rootNavController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            NavHost(
                navController = rootNavController,
                startDestination = RootTab.HOME.name,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(RootTab.HOME.name) {
                    HomeScreen()
                }
                composable(RootTab.ALARMS.name) {
                    //val vm = viewModel<AlarmViewModel>(factory = alarmViewModelFactory)
                    AlarmsNavHost(alarmViewModelFactory, rootNavController)
                }
                composable(RootTab.ADD_ALARM.name) {
                    val vm = viewModel<AlarmViewModel>(factory = alarmViewModelFactory)
                    AddAlarmView(vm)
                }
                composable(RootTab.SETTINGS.name) {
                    SettingsView()
                }
            }
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            RootTab.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { localRootNavigator.switchTab(it) }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            RootContent(modifier = Modifier.padding(innerPadding))
        }
    }
}



enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Home", Icons.Default.Home),
    ALARMS("Alarms", Icons.Default.Favorite),
    SETTINGS("Profile", Icons.Default.Settings),
    ADD_ALARM("Add Alarm", Icons.Default.AddCircle),
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
        AlarmList(
            PreviewAlarms2().alarmList,
            onAddClick = {},
            onDeleteClick = {},
            modifier = Modifier)
    }
}

@Preview(showBackground = true)
@Composable
fun AddAlarmViewPreview() {
    GoReadyTheme {
        //AddAlarmView(dummyViewModel)
    }
}