package com.tomcvt.goready.activities

import android.app.ComponentCaller
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import com.tomcvt.goready.application.AlarmApp
import com.tomcvt.goready.constants.EXTRA_ROUTINE_INFO
import com.tomcvt.goready.constants.EXTRA_ROUTINE_SESSION_ID
import com.tomcvt.goready.constants.EXTRA_ROUTINE_STEP
import com.tomcvt.goready.manager.AppRoutinesManager
import com.tomcvt.goready.repository.RoutineRepository
import com.tomcvt.goready.repository.RoutineStepRepository
import com.tomcvt.goready.repository.StepDefinitionRepository
import com.tomcvt.goready.ui.theme.GoReadyTheme
import com.tomcvt.goready.viewmodel.RoutinesViewModelFactory


private const val TAG = "RoutineFlowActivity"

class RoutineFlowActivity : ComponentActivity() {

    lateinit var appObject: AlarmApp
        private set

    lateinit var appRoutinesManager: AppRoutinesManager
        private set

    lateinit var routinesViewModelFactory: RoutinesViewModelFactory
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appObject = (application as AlarmApp)
        /*
        appRoutinesManager = AppRoutinesManager(
            RoutineRepository(appObject.db.routineDao()),
            RoutineStepRepository(appObject.db.routineStepDao()),
            StepDefinitionRepository(appObject.db.stepDefinitionDao())
        )
        routinesViewModelFactory = RoutinesViewModelFactory(appRoutinesManager)

         */

        val sessionId = intent.getLongExtra(EXTRA_ROUTINE_SESSION_ID, -1L)
        val stepNumber = intent.getIntExtra(EXTRA_ROUTINE_STEP, -1)
        val info = intent.getStringExtra(EXTRA_ROUTINE_INFO)

        val testString = "Session ID: $sessionId, Step ID: $stepNumber, Info: $info"

        setContent {
            GoReadyTheme {
                Text(testString)
            }
        }
    }

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        super.onNewIntent(intent, caller)
        val sessionId = intent.getLongExtra(EXTRA_ROUTINE_SESSION_ID, -1L)
        val stepNumber = intent.getIntExtra(EXTRA_ROUTINE_STEP, -1)
        val info = intent.getStringExtra(EXTRA_ROUTINE_INFO)
        val testString = "Session ID: $sessionId, Step ID: $stepNumber, Info: $info"
        setContent {
            GoReadyTheme {
                Text(testString)
            }
        }
    }
}