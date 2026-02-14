package com.tomcvt.goready

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.android.gms.ads.MobileAds
import com.tomcvt.goready.ads.ADMOB_ID_DYNAMIC_BANNER
import com.tomcvt.goready.ads.ADMOB_ID_TEST_BANNER
import com.tomcvt.goready.ads.BottomBarBannerAdView
import com.tomcvt.goready.ads.BottomBarDynamicAdView
import com.tomcvt.goready.application.AlarmApp
import com.tomcvt.goready.constants.EXTRA_ALARM_ID
import com.tomcvt.goready.manager.AppAlarmManager
import com.tomcvt.goready.manager.AppRoutinesManager
import com.tomcvt.goready.manager.SystemAlarmScheduler
import com.tomcvt.goready.premium.PremiumRepositoryI
import com.tomcvt.goready.premium.PremiumState
import com.tomcvt.goready.preview.PreviewAlarms2
import com.tomcvt.goready.repository.AlarmRepository
import com.tomcvt.goready.repository.RoutineRepository
import com.tomcvt.goready.repository.RoutineStepRepository
import com.tomcvt.goready.repository.StepDefinitionRepository
import com.tomcvt.goready.ui.composables.AddAlarmView
import com.tomcvt.goready.ui.composables.AlarmList
import com.tomcvt.goready.ui.composables.AlarmsNavHost
import com.tomcvt.goready.ui.composables.RoutineListRoute
import com.tomcvt.goready.ui.composables.SettingsView
import com.tomcvt.goready.ui.theme.GoReadyTheme
import com.tomcvt.goready.viewmodel.AlarmViewModel
import com.tomcvt.goready.viewmodel.AlarmViewModelFactory
import com.tomcvt.goready.viewmodel.RoutinesViewModel
import com.tomcvt.goready.viewmodel.RoutinesViewModelFactory
import com.tomcvt.goready.viewmodel.SettingsViewModel
import com.tomcvt.goready.viewmodel.SettingsViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    lateinit var appObject: AlarmApp
        private set

    lateinit var appAlarmManager: AppAlarmManager
        private set
    lateinit var appRoutinesManager: AppRoutinesManager
        private set

    lateinit var alarmViewModelFactory: AlarmViewModelFactory
        private set
    lateinit var routinesViewModelFactory: RoutinesViewModelFactory
        private set
    lateinit var settingsViewModelFactory: SettingsViewModelFactory
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appObject = (application as AlarmApp)

        appAlarmManager = AppAlarmManager(
            repository = AlarmRepository(appObject.db.alarmDao()),
            systemScheduler = SystemAlarmScheduler(this)
        )
        appRoutinesManager = AppRoutinesManager(
            RoutineRepository(appObject.db.routineDao()),
            RoutineStepRepository(appObject.db.routineStepDao()),
            StepDefinitionRepository(appObject.db.stepDefinitionDao())
        )

        alarmViewModelFactory = AlarmViewModelFactory(appAlarmManager, appRoutinesManager)
        routinesViewModelFactory = RoutinesViewModelFactory(appRoutinesManager)
        settingsViewModelFactory = SettingsViewModelFactory(appObject.premiumRepository)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
        //Debug window
        CoroutineScope(Dispatchers.IO).launch {
            val stepDefsAll = appRoutinesManager.getAllStepDefinitionsFlow().first()
            val stepDefsUser = appRoutinesManager.getUserStepDefinitionsFlow().first()
            Log.d("MainActivity", "Step definitions all: $stepDefsAll")
            Log.d("MainActivity", "Step definitions user: $stepDefsUser")
            MobileAds.initialize(this@MainActivity)
        }
        //end
        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.d("MainActivity", "SystemAlarmManager: $systemAlarmManager")
            val canSchedule = systemAlarmManager.canScheduleExactAlarms()
            Log.d("MainActivity", "AppAlarmManager: $appAlarmManager")
            Log.d("MainActivity", "Can schedule: $canSchedule")
        }

        //USE FULL SCREEN INTENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE)
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.FOREGROUND_SERVICE), 102)
            }
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

         */
        val startIntent = intent
        val alarmId = startIntent.getLongExtra(EXTRA_ALARM_ID, -1)
        if (alarmId != -1L) {
            Log.d("MainActivity", "Alarm ID to edit: $alarmId")
        }

        //TODO for now simple

        val premiumRepository = appObject.premiumRepository

        enableEdgeToEdge()
        setContent {
            GoReadyTheme {
                if (alarmId != -1L) {
                    GoReadyApp(
                        alarmViewModelFactory,
                        routinesViewModelFactory,
                        settingsViewModelFactory,
                        premiumRepository,
                        alarmId
                    )
                } else {
                    GoReadyApp(
                        alarmViewModelFactory,
                        routinesViewModelFactory,
                        settingsViewModelFactory,
                        premiumRepository
                    )
                }
            }
        }
    }
}

val LocalPremiumState = staticCompositionLocalOf<PremiumState> {
    error("No PremiumState provided")
}

@Composable
fun GoReadyApp(
    alarmViewModelFactory: AlarmViewModelFactory,
    routinesViewModelFactory: RoutinesViewModelFactory,
    settingsViewModelFactory: SettingsViewModelFactory,
    premiumRepository: PremiumRepositoryI,
    alarmId: Long? = null
) {
    val premiumState by premiumRepository.premiumState.collectAsState()
    CompositionLocalProvider(LocalPremiumState provides premiumState) {
        GoReadyAppMain(
            alarmViewModelFactory,
            routinesViewModelFactory,
            settingsViewModelFactory,
            alarmId
        )
    }
}

//@PreviewScreenSizes
@Composable
fun GoReadyAppMain(
    alarmViewModelFactory: AlarmViewModelFactory,
    routinesViewModelFactory: RoutinesViewModelFactory,
    settingsViewModelFactory: SettingsViewModelFactory,
    alarmId: Long? = null
) {
    val rootNavController = rememberNavController()
    val navbackStackEntry by rootNavController.currentBackStackEntryAsState()
    val currentRoute = navbackStackEntry?.destination?.route


    NavigationSuiteScaffold(
        navigationSuiteItems = {
            RootTab.entries.forEach { tab ->
                item(
                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                    label = { Text(tab.label) },
                    selected = currentRoute == tab.name, // NavController decides this
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
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Column (
            modifier = Modifier.fillMaxSize()
        ) {
            NavHost(
                navController = rootNavController,
                startDestination = RootTab.ALARMS.name,
                modifier = Modifier.weight(1f)
            ) {
                composable(RootTab.HOME.name) {
                    val vm = viewModel<RoutinesViewModel>(factory = routinesViewModelFactory)
                    RoutineListRoute(vm, rootNavController)
                }
                composable(RootTab.ALARMS.name) {
                    //val vm = viewModel<AlarmViewModel>(factory = alarmViewModelFactory)
                    AlarmsNavHost(alarmViewModelFactory, rootNavController)
                }
                composable(RootTab.ADD_ALARM.name) {
                    val vm = viewModel<AlarmViewModel>(factory = alarmViewModelFactory)
                    AddAlarmView(vm, rootNavController)
                    //AddAlarmRoute(vm, rootNavController) for now redundant
                }
                composable(RootTab.SETTINGS.name) {
                    val vm = viewModel<SettingsViewModel>(factory = settingsViewModelFactory)
                    SettingsView(vm)
                }
                composable(
                    route = "edit_alarm/{alarmId}",
                    arguments = listOf(navArgument("alarmId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val alarmId = backStackEntry.arguments?.getLong("alarmId")
                    val vm = viewModel<AlarmViewModel>(factory = alarmViewModelFactory)
                    AddAlarmView(vm, rootNavController, alarmId = alarmId)
                }
            }
            BottomBarBannerAdView(
                adUnitId = ADMOB_ID_TEST_BANNER,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    LaunchedEffect(Unit) {
        if (alarmId != null) {
            rootNavController.navigate("edit_alarm/$alarmId")
        }
    }
}

enum class RootTab(val label: String,
                   val icon: ImageVector,
) {
    HOME("Home", Icons.Default.Home),
    ALARMS("Alarms", Icons.Default.Home),
    SETTINGS("Profile", Icons.Default.Settings),
    ADD_ALARM("Add Alarm", Icons.Default.AddCircle);
}

@Composable
fun MainNavHost(
    alarmViewModelFactory: AlarmViewModelFactory,
    routinesViewModelFactory: RoutinesViewModelFactory,
    settingsViewModelFactory: SettingsViewModelFactory,
    alarmId: Long? = null
) {
    val rootNavController = rememberNavController()
    val navbackStackEntry by rootNavController.currentBackStackEntryAsState()
    val currentRoute = navbackStackEntry?.destination?.route

    Box (modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            NavHost(
                navController = rootNavController,
                startDestination = RootTab.ALARMS.name,
                modifier = Modifier.weight(1f)
            ) {
                composable(RootTab.HOME.name) {
                    val vm = viewModel<RoutinesViewModel>(factory = routinesViewModelFactory)
                    RoutineListRoute(vm, rootNavController)
                }
                composable(RootTab.ALARMS.name) {
                    //val vm = viewModel<AlarmViewModel>(factory = alarmViewModelFactory)
                    AlarmsNavHost(alarmViewModelFactory, rootNavController)
                }
                composable(RootTab.ADD_ALARM.name) {
                    val vm = viewModel<AlarmViewModel>(factory = alarmViewModelFactory)
                    AddAlarmView(vm, rootNavController)
                    //AddAlarmRoute(vm, rootNavController) for now redundant
                }
                composable(RootTab.SETTINGS.name) {
                    val vm = viewModel<SettingsViewModel>(factory = settingsViewModelFactory)
                    SettingsView(vm)
                }
                composable(
                    route = "edit_alarm/{alarmId}",
                    arguments = listOf(navArgument("alarmId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val alarmId = backStackEntry.arguments?.getLong("alarmId")
                    val vm = viewModel<AlarmViewModel>(factory = alarmViewModelFactory)
                    AddAlarmView(vm, rootNavController, alarmId = alarmId)
                }
            }
            /*
            BottomBarBannerAdView(
                adUnitId = ADMOB_ID_TEST_BANNER,
                modifier = Modifier.fillMaxWidth()
            )
             */
            BottomBarDynamicAdView(
                adUnitId = ADMOB_ID_DYNAMIC_BANNER,
                modifier = Modifier.fillMaxWidth()
            )
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
            onCardClick = {},
            onDebugClick = {},
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

