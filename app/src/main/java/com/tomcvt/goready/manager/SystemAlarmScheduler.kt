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
import com.tomcvt.goready.MainActivity
import com.tomcvt.goready.constants.ACTION_ALARM_SNOOZE_TRIGGERED
import com.tomcvt.goready.constants.ACTION_ALARM_TRIGGERED
import com.tomcvt.goready.constants.EXTRA_ALARM_ID
import com.tomcvt.goready.constants.EXTRA_REMAINING_SNOOZE
import com.tomcvt.goready.manager.AlarmReceiver
import com.tomcvt.goready.data.AlarmEntity
import java.time.Instant
import java.util.Calendar
import java.util.Date

class SystemAlarmScheduler(private val context: Context) {
    private val appContext = context.applicationContext
    val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager

    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    fun scheduleAlarm(alarm: AlarmEntity, alarmId: Long, remainingSnooze: Int = 0) {
        //val appAlarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as android.app.AppAlarmManager
        Log.d("AlarmScheduler", "Scheduling alarm with ID: $alarmId and snooze: $remainingSnooze")
        val intent = Intent(appContext
            , AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_REMAINING_SNOOZE, remainingSnooze)
            setAction(ACTION_ALARM_TRIGGERED)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        //printDebug(context, intent, alarmId, pendingIntent)

        var triggerTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        //TODO change to calendar logic
        if (triggerTime.timeInMillis <= System.currentTimeMillis()) {
            // If time has already passed today, set for tomorrow
            //triggerTime += 24 * 60 * 60 * 1000
            triggerTime.add(Calendar.DAY_OF_MONTH, 1)
        }
        var triggerTimeMillis = triggerTime.timeInMillis


        Log.d(
            "ALARM_DEBUG",
            "now=${Date(System.currentTimeMillis())}, " +
                    "alarm.hour=${alarm.hour}, alarm.minute=${alarm.minute}, " +
                    "calendar=${Date(triggerTimeMillis)}"
        )

        //triggerTime = System.currentTimeMillis() + 15000

        //val info = AppAlarmManager.AlarmClockInfo(triggerTime, pendingIntent)
        //appAlarmManager.setAlarmClock(info, pendingIntent)
        try {
            /*
            scheduleRTCWakeup(
                alarmId,
                triggerTime,
                pendingIntent
            )*/
            scheduleAlarmClock(
                alarmId,
                triggerTimeMillis,
                pendingIntent
            )
        } catch (e: SecurityException ) {
            Log.e("AlarmScheduler", "Security exception while scheduling alarm", e)
            //TODO add popup and ask user to grant permission
        }
    }

    fun scheduleNextAlarm(alarm: AlarmEntity, alarmId: Long, remainingSnooze: Int = 0, triggerTime: Long) {
        val intent = Intent(appContext
            , AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_REMAINING_SNOOZE, remainingSnooze)
            setAction(ACTION_ALARM_TRIGGERED)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        Log.d(TAG, "ALARM DEBUG: now=${Date(System.currentTimeMillis())}," +
                " alarm.hour=${alarm.hour}, alarm.minute=${alarm.minute}, " +
                "calendar=${Date(triggerTime)}")

        try {
            scheduleAlarmClock(
                alarmId,
                triggerTime,
                pendingIntent
            )
        } catch (e: SecurityException ) {
            Log.e("AlarmScheduler", "Security exception while scheduling alarm", e)
        }
    }

    fun scheduleSnooze(alarmId: Long, remainingSnooze: Int, snoozeTimeMinutes: Int) {
        val intent = Intent(appContext
            , AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_REMAINING_SNOOZE, remainingSnooze)
            setAction(ACTION_ALARM_SNOOZE_TRIGGERED)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        var triggerTime = Calendar.getInstance().apply {
            add(Calendar.MINUTE, snoozeTimeMinutes)
        }.timeInMillis

        if (triggerTime <= System.currentTimeMillis()) {
            Log.e("AlarmScheduler", "Snooze time is in the past, some bug occurred")
            return
        }

        Log.d("SNOOZE DEBUG", "now=${Date(System.currentTimeMillis())}, " +
                "alarm.hour=id${alarmId}, alarm.minute=id${alarmId}, " +
                "snoozeTimeMinutes=${snoozeTimeMinutes}, " +
                "calendar=${Date(triggerTime)}")

        try {
            scheduleAlarmClock(
                alarmId,
                triggerTime,
                pendingIntent
            )
        } catch (e: SecurityException ) {
            Log.e("AlarmScheduler", "Security exception while scheduling alarm", e)
            //TODO add popup and ask user to grant permission
        }
    }

    fun cancelAlarm(alarm: AlarmEntity) {
        val intent = Intent(appContext, AlarmReceiver::class.java).apply {
            setAction(ACTION_ALARM_TRIGGERED)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d("AlarmScheduler", "Intent Alarm cancelled with ID: ${alarm.id}")
    }

    fun scheduleTestAlarm(alarmId: Long) {
        val intent = Intent(appContext, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + 15000
        /*
        scheduleRTCWakeup(
            alarmId,
            triggerTime,
            pendingIntent
        )
         */
        scheduleAlarmClock(
            alarmId,
            triggerTime,
            pendingIntent
        )

    }


    fun scheduleRTCWakeup(alarmId: Long, triggerTime: Long, pendingIntent: PendingIntent) {
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

    fun scheduleAlarmClock(alarmId: Long, triggerTime: Long, pendingIntent: PendingIntent) {
        Log.d("AlarmScheduler", "Scheduling alarm with ID: ${alarmId} at ${triggerTime}")
        //TODO add showIntent, for now nothing
        val showIntent = Intent(appContext, MainActivity::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val showPending = PendingIntent.getActivity(appContext, 0, showIntent, PendingIntent.FLAG_IMMUTABLE)
        //TODO implement show intent
        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showPending)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
    }

    fun printDebug(
        context: Context,
        intent: Intent,
        alarmId: Long,
        pendingIntent: PendingIntent
    ) {
        Log.d("AlarmScheduler", "Intent context name: ${context}")
        Log.d("AlarmScheduler", "Intent package name: ${intent.component?.packageName.toString()}")
        Log.d("AlarmScheduler", "Intent action: ${intent.action.toString()}")
        Log.d("AlarmScheduler", "Intent data: ${intent.data.toString()}")
        Log.d("AlarmScheduler", "Intent extras: ${intent.extras.toString()}")
        Log.d("AlarmScheduler", "Scheduling alarm with ID: $alarmId and PendingIntent: $pendingIntent")
        Log.d("AlarmScheduler", "Scheduling alarm with ID: $alarmId and intent: $intent")
    }

    companion object {
        private const val TAG = "SystemAlarmScheduler"
    }
}