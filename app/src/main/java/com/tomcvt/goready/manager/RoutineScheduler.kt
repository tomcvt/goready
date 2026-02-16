package com.tomcvt.goready.manager

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.tomcvt.goready.constants.ACTION_ROUTINE_ABANDONED
import com.tomcvt.goready.constants.ACTION_STEP_TIMEOUT
import com.tomcvt.goready.constants.EXTRA_ROUTINE_ID
import com.tomcvt.goready.constants.EXTRA_ROUTINE_INFO
import com.tomcvt.goready.constants.EXTRA_ROUTINE_SESSION_ID
import com.tomcvt.goready.constants.EXTRA_ROUTINE_STEP
import java.util.Date

private const val TAG = "RoutineScheduler"
private const val MINUTE = 60000L
private const val NOTIF_ID = 1119

//TODO add variable timeout
class RoutineScheduler(private val context: Context) {

    val appContext: Context? = context.applicationContext
    val alarmManager = context.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleStepTimeout(sessionId: Long, routineId: Long, stepNumber: Int, timeoutMinutes: Int) : Int {
        val intent = Intent(appContext, RoutineReceiver::class.java).apply {
            putExtra(EXTRA_ROUTINE_SESSION_ID, sessionId)
            putExtra(EXTRA_ROUTINE_ID, routineId)
            putExtra(EXTRA_ROUTINE_STEP, stepNumber)
            setAction(ACTION_STEP_TIMEOUT)
        }
        val requestCode = hashRequestCode(sessionId)
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerTime = System.currentTimeMillis() + timeoutMinutes * MINUTE
        Log.d(TAG, "Scheduling step timeout for routine and step: $routineId , $stepNumber at ${Date(triggerTime)}")
        scheduleRTCWakeup(sessionId, triggerTime, pendingIntent)
        return requestCode
    }

    fun cancelStepTimeout(sessionId: Long) {
        val intent = Intent(appContext, RoutineReceiver::class.java).apply {
            //putExtra(EXTRA_ROUTINE_SESSION_ID, sessionId)
            setAction(ACTION_STEP_TIMEOUT)
        }
        val requestCode = hashRequestCode(sessionId)
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun scheduleUserAbandonedRoutine(sessionId: Long) : Int {
        val intent = Intent(appContext, RoutineReceiver::class.java).apply {
            putExtra(EXTRA_ROUTINE_SESSION_ID, sessionId)
            setAction(ACTION_ROUTINE_ABANDONED)
        }
        val requestCode = hashAbandonedCode(sessionId)
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        //TODO add variable timeout
        val triggerTime = System.currentTimeMillis() + 15 * MINUTE
        Log.d(TAG, "Scheduling user abandoned routine for session: $sessionId at ${Date(triggerTime)}")
        scheduleLowPriorityWakeup(sessionId, triggerTime, pendingIntent)
        return requestCode
    }


    private fun scheduleRTCWakeup(sessionId: Long, triggerTime: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                Log.d(TAG, "Exact alarm scheduled for session $sessionId at ${Date(triggerTime)}")
            } else {
                Log.e(TAG, "Can't schedule exact alarms, fallback to inexact")
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                Log.d(TAG, "normal alarm scheduled for session $sessionId at ${Date(triggerTime)}")
            }
        }
    }

    private fun scheduleLowPriorityWakeup(sessionId: Long, triggerTime: Long, pendingIntent: PendingIntent) {
        alarmManager.set(AlarmManager.RTC_WAKEUP,triggerTime,pendingIntent)
    }

    fun hashRequestCode(sessionId: Long) : Int { return sessionId.toInt() }
    fun hashAbandonedCode(sessionId: Long) : Int {return sessionId.toInt() + 10000}
}