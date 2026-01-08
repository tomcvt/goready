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
import com.tomcvt.goready.constants.EXTRA_ALARM_ID
import com.tomcvt.goready.service.AlarmForegroundService
import com.tomcvt.goready.ui.composables.*


class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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