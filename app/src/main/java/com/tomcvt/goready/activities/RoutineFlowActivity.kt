package com.tomcvt.goready.activities

import android.app.ComponentCaller
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.ads.MobileAds
import com.tomcvt.goready.application.AlarmApp
import com.tomcvt.goready.constants.ACTION_FINALIZE_ALARM
import com.tomcvt.goready.constants.ACTION_RF_UI_LAUNCHER
import com.tomcvt.goready.constants.ACTION_RF_UI_STEP_COMPLETE
import com.tomcvt.goready.constants.ACTION_RF_UI_SHOW
import com.tomcvt.goready.constants.ACTION_RF_UI_STEP_TIMEOUT
import com.tomcvt.goready.constants.EXTRA_ALARM_ID
import com.tomcvt.goready.constants.EXTRA_ROUTINE_ID
import com.tomcvt.goready.constants.EXTRA_ROUTINE_INFO
import com.tomcvt.goready.constants.EXTRA_ROUTINE_SESSION_ID
import com.tomcvt.goready.constants.EXTRA_ROUTINE_STEP
import com.tomcvt.goready.manager.RoutineFlowManager
import com.tomcvt.goready.manager.RoutineScheduler
import com.tomcvt.goready.repository.RoutineRepository
import com.tomcvt.goready.repository.RoutineSessionRepository
import com.tomcvt.goready.repository.RoutineStepRepository
import com.tomcvt.goready.repository.StepDefinitionRepository
import com.tomcvt.goready.service.AlarmForegroundService
import com.tomcvt.goready.ui.composables.RoutineFlowContent
import com.tomcvt.goready.ui.theme.GoReadyTheme
import com.tomcvt.goready.ui.theme.VibrantTheme
import com.tomcvt.goready.viewmodel.RoutineFlowViewModel
import com.tomcvt.goready.viewmodel.RoutineFlowViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


private const val TAG = "RoutineFlowActivity"

class RoutineFlowActivity : ComponentActivity() {

    lateinit var appObject: AlarmApp
        private set

    lateinit var routineFlowManager: RoutineFlowManager
        private set

    lateinit var routineFlowViewModelFactory: RoutineFlowViewModelFactory
        private set

    lateinit var routineScheduler: RoutineScheduler
        private set

    var userAnchored: Boolean = false
        private set

    var startedByAlarm: Long = -1L
        private set


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appObject = (application as AlarmApp)

        routineScheduler = RoutineScheduler(applicationContext)

        routineFlowManager = RoutineFlowManager(
            RoutineRepository(appObject.db.routineDao()),
            RoutineStepRepository(appObject.db.routineStepDao()),
            StepDefinitionRepository(appObject.db.stepDefinitionDao()),
            RoutineSessionRepository(appObject.db.routineSessionDao()),
            routineScheduler,
            applicationContext
        )

        CoroutineScope(Dispatchers.IO).launch {
            // Initialize the Google Mobile Ads SDK on a background thread.
            MobileAds.initialize(this@RoutineFlowActivity)
        }

        routineFlowViewModelFactory = RoutineFlowViewModelFactory(routineFlowManager)

        var onUserInitInteraction = {}

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId != -1L) {
            startedByAlarm = alarmId
            onUserInitInteraction = {
                finalizeAlarmOnInteraction()
            }
        }

        val sessionId = intent.getLongExtra(EXTRA_ROUTINE_SESSION_ID, -1L)
        val routineId = intent.getLongExtra(EXTRA_ROUTINE_ID, -1L)
        val stepNumber = intent.getIntExtra(EXTRA_ROUTINE_STEP, -1)
        val info = intent.getStringExtra(EXTRA_ROUTINE_INFO)
        val action = intent.action

        val onClose = {
            finish()
        }

        setContent {
            VibrantTheme {
                val vm = viewModel<RoutineFlowViewModel>(factory = routineFlowViewModelFactory)
                if (action == ACTION_RF_UI_LAUNCHER) {
                    vm.setLauncherRoutine(routineId)
                    vm.setLauncherOverlay(true)
                    Log.d(TAG, "onCreate: action, routineId: $action, $routineId")
                }
                if (action in showFlowActions) {
                    vm.selectSession(sessionId)
                    vm.setLauncherOverlay(false)
                    Log.d(TAG, "onCreate: action, sessionId: $action, $sessionId")
                }
                RoutineFlowContent(vm, onClose, onUserInitInteraction)
            }
        }
    }

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        super.onNewIntent(intent, caller)

        var onUserInitInteraction = {}

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId != -1L) {
            startedByAlarm = alarmId
            onUserInitInteraction = {
                finalizeAlarmOnInteraction()
            }
        }

        val sessionId = intent.getLongExtra(EXTRA_ROUTINE_SESSION_ID, -1L)
        val routineId = intent.getLongExtra(EXTRA_ROUTINE_ID, -1L)
        val stepNumber = intent.getIntExtra(EXTRA_ROUTINE_STEP, -1)
        val info = intent.getStringExtra(EXTRA_ROUTINE_INFO)
        val action = intent.action
        Log.d(TAG, "onNewIntent: action, rId: $action, $sessionId, $routineId, $stepNumber, $info")

        val onClose = {
            finish()
        }

        setContent {
            VibrantTheme {
                val vm = viewModel<RoutineFlowViewModel>(factory = routineFlowViewModelFactory)
                if (action == ACTION_RF_UI_LAUNCHER) {
                    vm.setLauncherRoutine(routineId)
                    vm.setLauncherOverlay(true)
                    Log.d(TAG, "onCreate: action, routineId: $action, $routineId")
                }
                if (action in showFlowActions) {
                    vm.selectSession(sessionId)
                    vm.setLauncherOverlay(false)
                    Log.d(TAG, "onCreate: action, sessionId: $action, $sessionId")
                }
                RoutineFlowContent(vm, onClose, onUserInitInteraction)
            }
        }
    }

    private fun finalizeAlarmOnInteraction() {
        if (!userAnchored && startedByAlarm != -1L) {
            userAnchored = true
            finalizeAlarm(startedByAlarm)
        }
    }

    private fun finalizeAlarm(alarmId: Long) {
        val intent = Intent(this, AlarmForegroundService::class.java)
        intent.action = ACTION_FINALIZE_ALARM
        intent.putExtra(EXTRA_ALARM_ID, alarmId)
        Log.d(TAG, "finalizeAlarm: $alarmId")
        startService(intent)
    }

    //TODO implement service receive and stopping the service when the user interacted with the alarm

}

@Composable
fun RoutineFlowHost(
    viewModel: RoutineFlowViewModel,
    onClose: () -> Unit,
    onUserInitInteraction: () -> Unit
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            RoutineFlowContent(viewModel, onClose, onUserInitInteraction)
        }
    }
}




private val showFlowActions = listOf(
    ACTION_RF_UI_SHOW,
    ACTION_RF_UI_STEP_COMPLETE,
    ACTION_RF_UI_STEP_TIMEOUT
    //TODO add intents and in
)






