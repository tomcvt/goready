package com.tomcvt.goready.manager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tomcvt.goready.application.AlarmApp
import com.tomcvt.goready.constants.ACTION_FF_TO_NEXT_STEP
import com.tomcvt.goready.constants.ACTION_ROUTINE_COMPLETE
import com.tomcvt.goready.constants.ACTION_STEP_TIMEOUT
import com.tomcvt.goready.constants.EXTRA_ROUTINE_ID
import com.tomcvt.goready.constants.EXTRA_ROUTINE_INFO
import com.tomcvt.goready.constants.EXTRA_ROUTINE_SESSION_ID
import com.tomcvt.goready.constants.EXTRA_ROUTINE_STEP
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RoutineReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as AlarmApp
        val context = context.applicationContext

        val routineFlowManager = RoutineFlowManager(
            app.routineRepository,
            app.routineStepRepository,
            app.stepDefinitionRepository,
            app.routineSessionRepository,
            app.routineScheduler,
            context
        )

        val action = intent.action
        //val info = intent.getStringExtra(EXTRA_ROUTINE_INFO)
        val sessionId = intent.getLongExtra(EXTRA_ROUTINE_SESSION_ID, -1L)
        val routineId = intent.getLongExtra(EXTRA_ROUTINE_ID, -1L)
        val routineStep = intent.getIntExtra(EXTRA_ROUTINE_STEP, -1)

        if (action == "ACTION_START_ROUTINE") {
            CoroutineScope(Dispatchers.IO).launch {
                routineFlowManager.startRoutine(routineId)
            }
        }

        if (action == ACTION_STEP_TIMEOUT) {
            CoroutineScope(Dispatchers.IO).launch {
                routineFlowManager.stepFinishedTimeout(sessionId, routineId, routineStep)
            }
        }

        if (action == ACTION_FF_TO_NEXT_STEP) {
            CoroutineScope(Dispatchers.IO).launch {
                routineFlowManager.advanceToNextStep(sessionId)
                routineFlowManager.startStep(sessionId)
            }
        }

        if (action == ACTION_ROUTINE_COMPLETE) {
            CoroutineScope(Dispatchers.IO).launch {
                routineFlowManager.finishRoutine(sessionId)
            }
        }

    }
}