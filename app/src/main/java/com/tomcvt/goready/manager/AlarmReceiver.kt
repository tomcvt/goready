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
import com.tomcvt.goready.application.AlarmApp
import com.tomcvt.goready.constants.ACTION_ALARM_TRIGGERED
import com.tomcvt.goready.constants.EXTRA_ALARM_ID
import com.tomcvt.goready.constants.EXTRA_REMAINING_SNOOZE
import com.tomcvt.goready.service.AlarmForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    //TODO !!!! after alarm intent received,
    // -> if next days enabled, schedule next, if not next days disable, extra is first alarm, if yes do the things and check if enabled, if not dont check enabled
    companion object {
        private const val TAG = "AlarmReceiver"
    }


    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as AlarmApp
        val context = context.applicationContext

        val appAlarmManager = AppAlarmManager(app.alarmRepository, app.systemAlarmScheduler)

        val action = intent.action
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        val remainingSnooze = intent.getIntExtra(EXTRA_REMAINING_SNOOZE, -1)
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                appAlarmManager.scheduleAllEnabledAlarms()
            }
            return
        }
        if (action == ACTION_ALARM_TRIGGERED) {
            CoroutineScope(Dispatchers.IO).launch {
                appAlarmManager.scheduleNextAlarmOrDisable(alarmId)
            }
            //showNotification(context, alarmId)
        }

        Log.d(TAG, "Alarm received with ID: $alarmId and snooze: $remainingSnooze")
        Log.d(TAG, "Intent action: ${intent.action.toString()}")

        val serviceIntent = Intent(context, AlarmForegroundService::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_REMAINING_SNOOZE, remainingSnooze)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }


    //TODO implement next alarm notification
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