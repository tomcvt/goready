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

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager

    fun scheduleAlarm(alarm: AlarmEntity, alarmId: Long) {

        Log.d("AlarmScheduler", "Scheduling alarm with ID: $alarmId")
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),  // unique per alarm
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

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
        //if (true/*alarmManager.canScheduleExactAlarms()*/ ) {

        //for now we not care about checking can schedule because too high api, just require permission
        /*
        try {
            context.checkSelfPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
        } catch (e: SecurityException) {
            throw SecurityException("SCHEDULE_EXACT_ALARM permission is required to schedule exact alarms.", e)
        }*/

        /*
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        context.startActivity(intent)
        //later ping user to settings if not granted
         */
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
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d("AlarmScheduler", "Intent Alarm cancelled with ID: ${alarm.id}")
    }
}