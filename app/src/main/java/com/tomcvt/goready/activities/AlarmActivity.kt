package com.tomcvt.goready.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.tomcvt.goready.BuildConfig
import com.tomcvt.goready.application.AlarmApp
import com.tomcvt.goready.constants.EXTRA_ALARM_ID
import com.tomcvt.goready.constants.TaskType
import com.tomcvt.goready.manager.AppAlarmManager
import com.tomcvt.goready.manager.SystemAlarmScheduler
import com.tomcvt.goready.service.AlarmForegroundService
import com.tomcvt.goready.ui.composables.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = (application as AlarmApp)
        val repository = app.alarmRepository
        val appAlarmManager = AppAlarmManager(repository, SystemAlarmScheduler(this))

        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Show over lock screen + turn screen on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        onBackPressedDispatcher.addCallback(this) {
            Log.d("AlarmActivity", "Back pressed")
        }

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1)

        lifecycleScope.launch {
            val alarmEntity = withContext(Dispatchers.IO) {appAlarmManager.getAlarm(alarmId)}
            Log.d("AlarmActivity", "Alarm entity: $alarmEntity")

            if (alarmEntity == null) {
                stopAlarmService()
                finish()
                return@launch
            }
            var taskType  = TaskType.valueOf(alarmEntity.task?: "NONE")
            Log.d("AlarmActivity", "Task type: $taskType")
            val data = alarmEntity.taskData
            Log.d("AlarmActivity", "Task data: $data")

            if (data.isNullOrEmpty()) {
                taskType = TaskType.NONE
            }

            //TODO: how to get the alarm entity context here
            //TODO differentiate composables based on alarm type
            enableEdgeToEdge()
            setContent {
                when (taskType) {
                    TaskType.NONE -> {
                        TestAlarmScreen(
                            alarmId = alarmId,
                            onStopAlarm = {
                                stopAlarmService()
                                finish()
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    TaskType.TEXT -> {
                        if (BuildConfig.IS_ALARM_TEST) {
                            DebugTextAlarmScreen(
                                text = data?: "Turn off the alarm",
                                onStopAlarm = {
                                    stopAlarmService()
                                    finish()
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    TaskType.TIMER -> {

                    }
                    TaskType.COUNTDOWN -> {

                    }
                    TaskType.MATH -> {

                    }
                }

            }
        }
    }

    override fun onBackPressed() {
        // Intentionally block back
    }

    private fun stopAlarmService() {
        val intent = Intent(this, AlarmForegroundService::class.java)
        intent.action = "STOP_ALARM"
        startService(intent)
        //stopService(intent)
    }
}