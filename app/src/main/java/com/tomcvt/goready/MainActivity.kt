package com.tomcvt.goready

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tomcvt.goready.application.AlarmApp
import com.tomcvt.goready.manager.AppAlarmManager
import com.tomcvt.goready.manager.SystemAlarmScheduler
import com.tomcvt.goready.preview.PreviewAlarms2
import com.tomcvt.goready.repository.AlarmRepository
import com.tomcvt.goready.ui.composables.AddAlarmView
import com.tomcvt.goready.ui.composables.AlarmList
import com.tomcvt.goready.ui.composables.AlarmsNavHost
import com.tomcvt.goready.ui.composables.HomeScreen
import com.tomcvt.goready.ui.composables.SettingsView
import com.tomcvt.goready.ui.navigation.LocalRootNavigator
import com.tomcvt.goready.ui.navigation.RootNavigatorImpl
import com.tomcvt.goready.ui.navigation.RootTab
import com.tomcvt.goready.ui.theme.GoReadyTheme
import com.tomcvt.goready.viewmodel.AlarmViewModel
import com.tomcvt.goready.viewmodel.AlarmViewModelFactory

class MainActivity : ComponentActivity() {

    lateinit var appObject: AlarmApp
        private set

    lateinit var appAlarmManager: AppAlarmManager
        private set

    lateinit var alarmViewModelFactory: AlarmViewModelFactory
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appObject = (application as AlarmApp)

        appAlarmManager = AppAlarmManager(
            repository = AlarmRepository(appObject.db.alarmDao()),
            systemScheduler = SystemAlarmScheduler(this)
        )
        alarmViewModelFactory = AlarmViewModelFactory(appAlarmManager)
        val systemAlarmManager = this.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.d("MainActivity", "SystemAlarmManager: $systemAlarmManager")
            val canSchedule = systemAlarmManager.canScheduleExactAlarms()
            Log.d("MainActivity", "AppAlarmManager: $appAlarmManager")
            Log.d("MainActivity", "Can schedule: $canSchedule")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
        //USE FULL SCREEN INTENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE)
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.FOREGROUND_SERVICE), 102)
            }
        }
        Log.d("MainActivity", "Foreground service permission")
        val foregroundPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE)
        if (foregroundPermission != PackageManager.PERMISSION_GRANTED) {
            Log.d("MainActivity", "Foreground service permission not granted")
        } else {
            Log.d("MainActivity", "Foreground service permission granted")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.USE_FULL_SCREEN_INTENT)
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.USE_FULL_SCREEN_INTENT), 104)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.SCHEDULE_EXACT_ALARM)
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SCHEDULE_EXACT_ALARM), 103)
            }
            if (!systemAlarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                this.startActivity(intent)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Permission check for battery optimization
            val permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                return
            }
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }
        //SYSTEM EXEMPTED FOREGROUND SERVICE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_SYSTEM_EXEMPTED)
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.FOREGROUND_SERVICE_SYSTEM_EXEMPTED), 105)
            }
            val permissionCheck2 = ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK)
            if (permissionCheck2 != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK),
                    106
                )
            }
        }


        enableEdgeToEdge()
        setContent {
            //val alarmViewModel: AlarmViewModel = AlarmViewModelProvider.provideAlarmViewModel(this)
            val rootNavigator = remember { RootNavigatorImpl(start = RootTab.HOME) }
            //TODO root navigator and comp local provider not needed here
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
    val rootNavController = rememberNavController()
    val navbackStackEntry by rootNavController.currentBackStackEntryAsState()
    val currentRootTab = RootTab.valueOf(
        navbackStackEntry?.destination?.route ?: RootTab.HOME.name
    )




    NavigationSuiteScaffold(
        navigationSuiteItems = {
            RootTab.entries.forEach { tab ->
                item(
                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                    label = { Text(tab.label) },
                    selected = currentRootTab == tab, // NavController decides this
                    onClick = {
                        rootNavController.navigate(tab.name) {
                            popUpTo(rootNavController.graph.startDestinationId) {
                                saveState = false
                            }
                            launchSingleTop = true
                            restoreState = false
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
                    AlarmsNavHost(alarmViewModelFactory, rootNavController) //navhost for future extension with separate vm
                }
                composable(RootTab.ADD_ALARM.name) {
                    val vm = viewModel<AlarmViewModel>(factory = alarmViewModelFactory)
                    AddAlarmView(vm, rootNavController)
                    //AddAlarmRoute(vm, rootNavController) for now redundant
                }
                composable(RootTab.SETTINGS.name) {
                    val vm = viewModel<AlarmViewModel>(factory = alarmViewModelFactory)
                    SettingsView()
                }
            }
        }
    }
}



enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Home", Icons.Default.Home),
    ALARMS("Alarms", Icons.Default.Favorite),
    SETTINGS("Settings", Icons.Default.Settings),
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
            onAlarmSwitchChange = { _, _ -> },
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