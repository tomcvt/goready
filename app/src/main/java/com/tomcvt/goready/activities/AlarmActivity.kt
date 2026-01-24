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
import com.tomcvt.goready.constants.EXTRA_REMAINING_SNOOZE
import com.tomcvt.goready.constants.SNOOZE_COUNTS
import com.tomcvt.goready.constants.SNOOZE_MINUTES
import com.tomcvt.goready.constants.TaskType
import com.tomcvt.goready.manager.AppAlarmManager
import com.tomcvt.goready.manager.SystemAlarmScheduler
import com.tomcvt.goready.service.AlarmForegroundService
import com.tomcvt.goready.ui.composables.*
import com.tomcvt.goready.ui.theme.VibrantUniTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val TAG = "AlarmActivity"
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
        val receivedRemainingSnooze = intent.getIntExtra(EXTRA_REMAINING_SNOOZE, -1)
        Log.d(TAG, "Alarm ID: $alarmId, snooze: $receivedRemainingSnooze")


        val testAlarm = intent.getBooleanExtra("TestAlarm", false)
        var onInteraction = {
            Log.d("AlarmActivity", "Sending interaction intent")
            sendInteraction()
        }
        if (testAlarm) {
            onInteraction = {
                Log.d("AlarmActivity", "Test Interaction")
            }
        }
        var stopAlarm = {
            stopAlarmService()
            finish()
        }
        if (testAlarm) {
            stopAlarm = {
                Log.d("AlarmActivity", "Test stop alarm")
                finish()
            }
        }

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
            val snoozeTime = alarmEntity.snoozeDurationMinutes?: -1
            Log.d("AlarmActivity", "Snooze time: $snoozeTime")


            if (data.isNullOrEmpty()) {
                taskType = TaskType.NONE
            }
            var canSnooze = receivedRemainingSnooze > 0
            if (snoozeTime !in SNOOZE_MINUTES || !alarmEntity.snoozeEnabled) {
                canSnooze = false
            }
            var onSnooze = {
                Log.d("AlarmActivity", "Snoozing")
                val remainingSnooze = receivedRemainingSnooze - 1
                appAlarmManager.scheduleSnoozeById(alarmId, remainingSnooze, snoozeTime)
                //TODO for now stop service and check if it reliably relaunches
                stopAlarmService()
                finish()
            }
            if (testAlarm) {
                onSnooze = {
                    Log.d("AlarmActivity", "Test snooze")
                }
            }
            Log.d(TAG, "Can snooze: $canSnooze")

            //TODO: how to get the alarm entity context here
            //TODO differentiate composables based on alarm type
            enableEdgeToEdge()
            setContent {
                VibrantUniTheme {
                    AlarmScreen(
                        alarmId = alarmId,
                        alarmName = alarmEntity.label ?: "Alarm",
                        taskType = taskType,
                        taskData = data,
                        onStopAlarm = stopAlarm,
                        onInteraction = onInteraction,
                        modifier = Modifier.fillMaxSize(),
                        //TODO update later snooze,
                        canSnooze = canSnooze,
                        snoozeTime = snoozeTime,
                        onSnooze = { onSnooze() }
                    )
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
    private fun sendInteraction() {
        val intent = Intent(this, AlarmForegroundService::class.java)
        intent.action = "USER_INTERACTION"
        startService(intent)
    }
}