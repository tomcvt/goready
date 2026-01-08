package com.tomcvt.goready.manager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.tomcvt.goready.R
import com.tomcvt.goready.constants.EXTRA_ALARM_ID
import com.tomcvt.goready.service.AlarmForegroundService

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        Log.d("AlarmReceiver", "Alarm received with ID: $alarmId")
        // Here you can trigger a notification, play a sound, vibrate, etc.
        showNotification(context, alarmId)

        val serviceIntent = Intent(context, AlarmForegroundService::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    private fun showNotification(context: Context, alarmId: Long) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "alarm_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alarms",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Alarm")
            .setContentText("Your alarm #$alarmId is ringing!")
            .setSmallIcon(R.drawable.ic_alarm)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(alarmId.toInt(), notification)
    }
}