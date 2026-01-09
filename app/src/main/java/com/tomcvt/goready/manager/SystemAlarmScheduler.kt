package com.tomcvt.goready.manager

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresPermission
import com.tomcvt.goready.constants.EXTRA_ALARM_ID
import com.tomcvt.goready.manager.AlarmReceiver
import com.tomcvt.goready.data.AlarmEntity
import java.util.Calendar

class SystemAlarmScheduler(private val context: Context) {

    //private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
    private val appContext = context.applicationContext
    //private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager


    fun scheduleAlarm(alarm: AlarmEntity, alarmId: Long) {
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        Log.d("AlarmScheduler", "Scheduling alarm with ID: $alarmId")
        val intent = Intent(context.applicationContext
            , AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context.applicationContext,
            alarm.id.toInt(),  // unique per alarm
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        Log.d("AlarmScheduler", "Intent package name: ${intent.`package`.toString()}")
        Log.d("AlarmScheduler", "Intent action: ${intent.action.toString()}")
        Log.d("AlarmScheduler", "Intent data: ${intent.data.toString()}")
        Log.d("AlarmScheduler", "Intent extras: ${intent.extras.toString()}")


        // For simplicity, exact alarm at hour:minute
        var triggerTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        if (triggerTime <= System.currentTimeMillis()) {
            // If time has already passed today, set for tomorrow
            triggerTime += 24 * 60 * 60 * 1000
        }
        Log.d("AlarmScheduler", "Scheduling alarm with ID: ${alarmId} at ${triggerTime}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                Log.d("AlarmScheduler", "Exact alarm scheduled with ID: ${alarmId}")
            } else {
                // If we can't schedule exact, fallback to inexact or ask user
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                // Optional: Redirect user to settings
                Log.d("AlarmScheduler", "Can't schedule exact alarms, fallback to inexact")
                Log.d("AlarmScheduler", "normal alarm scheduled with ID: ${alarmId}")
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                context.startActivity(intent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
            Log.d("AlarmScheduler", " (low android sdk) Exact alarm scheduled with ID: ${alarmId}")
        }
    }

    fun cancelAlarm(alarm: AlarmEntity) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context.applicationContext,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        alarmManager.cancel(pendingIntent)
        Log.d("AlarmScheduler", "Intent Alarm cancelled with ID: ${alarm.id}")
    }
}