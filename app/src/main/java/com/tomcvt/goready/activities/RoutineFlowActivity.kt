package com.tomcvt.goready.activities

import android.app.ComponentCaller
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tomcvt.goready.application.AlarmApp
import com.tomcvt.goready.constants.ACTION_RF_UI_LAUNCHER
import com.tomcvt.goready.constants.ACTION_RF_UI_STEP_COMPLETE
import com.tomcvt.goready.constants.ACTION_RF_UI_SHOW
import com.tomcvt.goready.constants.ACTION_RF_UI_STEP_TIMEOUT
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
import com.tomcvt.goready.ui.composables.RoutineFlowContent
import com.tomcvt.goready.ui.theme.GoReadyTheme
import com.tomcvt.goready.ui.theme.VibrantTheme
import com.tomcvt.goready.viewmodel.RoutineFlowViewModel
import com.tomcvt.goready.viewmodel.RoutineFlowViewModelFactory


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

        routineFlowViewModelFactory = RoutineFlowViewModelFactory(routineFlowManager)

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
                RoutineFlowContent(vm, onClose)
            }
        }
    }

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        super.onNewIntent(intent, caller)
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
                RoutineFlowContent(vm, onClose)
            }
        }
    }
}

private val showFlowActions = listOf(
    ACTION_RF_UI_SHOW,
    ACTION_RF_UI_STEP_COMPLETE,
    ACTION_RF_UI_STEP_TIMEOUT
    //TODO add intents and in
)




