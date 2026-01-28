package com.tomcvt.goready.manager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tomcvt.goready.application.AlarmApp
import com.tomcvt.goready.constants.EXTRA_ROUTINE_ID
import com.tomcvt.goready.constants.EXTRA_ROUTINE_INFO
import com.tomcvt.goready.constants.EXTRA_ROUTINE_STEP

class RoutineReceiver : BroadcastReceiver() {


    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as AlarmApp

        val routineFlowManager = app.routineFlowManager

        val info = intent.getStringExtra(EXTRA_ROUTINE_INFO)
        val routineId = intent.getLongExtra(EXTRA_ROUTINE_ID, -1L)
        val routineStep = intent.getIntExtra(EXTRA_ROUTINE_STEP, -1)

        if (info == "START_ROUTINE") {
            routineFlowManager.startRoutine(routineId)
        }

    }
}