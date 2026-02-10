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
import com.tomcvt.goready.application.AlarmApp
import com.tomcvt.goready.constants.ACTION_RF_UI_LAUNCHER
import com.tomcvt.goready.constants.ACTION_STOP_ALARM_SOUND
import com.tomcvt.goready.constants.ACTION_UI_HIDDEN
import com.tomcvt.goready.constants.EXTRA_ALARM_ID
import com.tomcvt.goready.constants.EXTRA_REMAINING_SNOOZE
import com.tomcvt.goready.constants.EXTRA_ROUTINE_ID
import com.tomcvt.goready.constants.SNOOZE_MINUTES
import com.tomcvt.goready.constants.TaskType
import com.tomcvt.goready.manager.AppAlarmManager
import com.tomcvt.goready.manager.SystemAlarmScheduler
import com.tomcvt.goready.service.AlarmForegroundService
import com.tomcvt.goready.ui.composables.*
import com.tomcvt.goready.ui.theme.VibrantLightTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "AlarmActivity"
class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = (application as AlarmApp)
        val repository = app.alarmRepository
        val appAlarmManager = AppAlarmManager(repository, SystemAlarmScheduler(this))

        val routineRepository = app.routineRepository

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
            Log.d(TAG, "Back pressed")
        }

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1)
        val receivedRemainingSnooze = intent.getIntExtra(EXTRA_REMAINING_SNOOZE, -1)
        Log.d(TAG, "Alarm ID: $alarmId, snooze: $receivedRemainingSnooze")


        val testAlarm = intent.getBooleanExtra("TestAlarm", false)
        var onInteraction = {
            Log.d(TAG, "Sending interaction intent")
            sendInteraction()
        }
        if (testAlarm) {
            onInteraction = {
                Log.d(TAG, "Test Interaction")
            }
        }
        var stopAlarm = {
            stopAlarmService()
            finish()
        }
        if (testAlarm) {
            stopAlarm = {
                Log.d(TAG, "Test stop alarm")
                finish()
            }
        }

        lifecycleScope.launch {
            val alarmEntity = withContext(Dispatchers.IO) {appAlarmManager.getAlarm(alarmId)}
            Log.d(TAG, "Alarm entity: $alarmEntity")

            if (alarmEntity == null) {
                stopAlarmService()
                finish()
                return@launch
            }
            var taskType  = TaskType.valueOf(alarmEntity.task?: "NONE")
            Log.d(TAG, "Task type: $taskType")
            val data = alarmEntity.taskData
            Log.d(TAG, "Task data: $data")
            val snoozeTime = alarmEntity.snoozeDurationMinutes?: -1
            Log.d(TAG, "Snooze time: $snoozeTime")
            val routineId = alarmEntity.routineId
            if (routineId != null) {
                val routine = withContext(Dispatchers.IO) {routineRepository.getRoutineById(routineId)}
                if (routine == null) {
                    //TODO handle error somewhere (not existing id)
                    Log.w(TAG, "Routine with id $routineId does not exist")
                } else {
                    Log.d(TAG, "Routine to launch after alarm: $routine")
                    stopAlarm = {
                        launchRoutine(routineId, alarmId)
                        stopAlarmSound()
                        finish()
                    }
                }
            }


            if (data.isNullOrEmpty()) {
                taskType = TaskType.NONE
            }
            var canSnooze = receivedRemainingSnooze > 0
            if (snoozeTime !in SNOOZE_MINUTES || !alarmEntity.snoozeEnabled) {
                canSnooze = false
            }
            var onSnooze = {
                Log.d(TAG, "Snoozing")
                val remainingSnooze = receivedRemainingSnooze - 1
                appAlarmManager.scheduleSnoozeById(alarmId, remainingSnooze, snoozeTime)
                //TODO for now stop service and check if it reliably relaunches
                stopAlarmService()
                finish()
            }
            if (testAlarm) {
                onSnooze = {
                    Log.d(TAG, "Test snooze")
                }
            }
            Log.d(TAG, "Can snooze: $canSnooze")

            //TODO: how to get the alarm entity context here
            //TODO differentiate composables based on alarm type
            enableEdgeToEdge()
            setContent {
                VibrantLightTheme {
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

    override fun onStop() {
        super.onStop()
        pingServiceUiHidden()
    }

    private fun stopAlarmService() {
        val intent = Intent(this, AlarmForegroundService::class.java)
        intent.action = "STOP_ALARM"
        startService(intent)
        //stopService(intent)
    }
    private fun stopAlarmSound() {
        val intent = Intent(this, AlarmForegroundService::class.java)
        intent.action = ACTION_STOP_ALARM_SOUND
        startService(intent)
    }
    private fun sendInteraction() {
        val intent = Intent(this, AlarmForegroundService::class.java)
        intent.action = "USER_INTERACTION"
        startService(intent)
    }

    private fun launchRoutine(routineId: Long, alarmId: Long) {
        val launchIntent = Intent(this, RoutineFlowActivity::class.java).apply {
            action = ACTION_RF_UI_LAUNCHER
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(EXTRA_ROUTINE_ID, routineId)
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        startActivity(launchIntent)
    }
    private fun pingServiceUiHidden() {
        val intent = Intent(this, AlarmForegroundService::class.java)
        intent.action = ACTION_UI_HIDDEN
        startService(intent)
    }
}