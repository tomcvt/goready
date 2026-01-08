package com.tomcvt.goready.activities

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.tomcvt.goready.service.AlarmForegroundService
import com.tomcvt.goready.ui.composables.*


class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Show over lock screen + turn screen on
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        val alarmId = intent.getLongExtra("alarm_id", -1)
        //TODO: how to get the alarm entity context here
        //TODO differentiate composables based on alarm type
        enableEdgeToEdge()
        setContent {
            TestAlarmScreen(
                alarmId = alarmId,
                onStopAlarm = {
                    stopAlarmService()
                    finish()
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    override fun onBackPressed() {
        // Intentionally block back
    }

    private fun stopAlarmService() {
        val intent = Intent(this, AlarmForegroundService::class.java)
        stopService(intent)
    }
}