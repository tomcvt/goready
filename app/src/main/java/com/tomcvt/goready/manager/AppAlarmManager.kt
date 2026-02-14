package com.tomcvt.goready.manager

import android.util.Log
import com.tomcvt.goready.data.AlarmEntity
import com.tomcvt.goready.domain.AlarmDraft
import com.tomcvt.goready.domain.SimpleAlarmDraft
import com.tomcvt.goready.repository.AlarmRepository
import java.time.DayOfWeek
import java.util.Calendar

open class AppAlarmManager(private val repository: AlarmRepository, private val systemScheduler: SystemAlarmScheduler) {
    fun getAlarmsFlow() = repository.getAlarms()
    suspend fun createAlarm(draft: AlarmDraft) {
        // 1. Convert draft → entity
        val entity = AlarmEntity(
            hour = draft.hour,
            minute = draft.minute,
            label = draft.label,
            repeatDays = draft.repeatDays,
            isEnabled = true,
            task = draft.task,
            taskData = draft.taskData,
            soundUri = draft.soundUri,
            snoozeEnabled = draft.snoozeEnabled,
            snoozeDurationMinutes = draft.snoozeDurationMinutes,
            snoozeMaxCount = draft.snoozeMaxCount,
            routineId = draft.routineId
        )

        // 2. Save to DB
        val newAlarmId = repository.insertAlarm(entity)

        // 3. Schedule system alarm
        try {
            val remainingSnooze = if (draft.snoozeEnabled) draft.snoozeMaxCount!! else 0
            systemScheduler.scheduleAlarm(entity, newAlarmId, remainingSnooze)
            //systemScheduler.scheduleAlarm(entity, newAlarmId)
        } catch (e: SecurityException) {
            // Handle the exception, e.g., show an error message to the user
            Log.e("AppAlarmManager", "Security exception while scheduling alarm", e)
            //TODO add popup and ask user to grant permission
            //throw e
            //TODO catch exception in activity and show error message

        }
    }

    fun scheduleSnoozeById(alarmId: Long, remainingSnooze: Int, snoozeTimeMinutes: Int) {
        systemScheduler.scheduleSnooze(alarmId, remainingSnooze, snoozeTimeMinutes)
    }

    suspend fun updateAlarm(draft: AlarmDraft, alarmId: Long) {
        val oldAlarm = repository.getAlarmById(alarmId) ?: return
        val updatedAlarm = oldAlarm.copy(
            hour = draft.hour,
            minute = draft.minute,
            label = draft.label,
            repeatDays = draft.repeatDays,
            task = draft.task,
            taskData = draft.taskData,
            soundUri = draft.soundUri,
            snoozeEnabled = draft.snoozeEnabled,
            snoozeDurationMinutes = draft.snoozeDurationMinutes,
            snoozeMaxCount = draft.snoozeMaxCount,
            routineId = draft.routineId
        )
        //if (oldAlarm.hour != updatedAlarm.hour || oldAlarm.minute != updatedAlarm.minute) {

        try {
            if (updatedAlarm.isEnabled) {
                val remainingSnooze = if (updatedAlarm.snoozeEnabled) updatedAlarm.snoozeMaxCount!! else 0
                systemScheduler.cancelAlarm(oldAlarm)
                systemScheduler.scheduleAlarm(updatedAlarm, alarmId, remainingSnooze)
            } else {
                systemScheduler.cancelAlarm(oldAlarm)
            }
        } catch (e: SecurityException) {
            // Handle the exception, e.g., show an error message to the user
            Log.e("AppAlarmManager", "Security exception while scheduling alarm", e)
            //TODO add popup and ask user to grant permission
        }

        repository.updateAlarm(updatedAlarm)
    }


    suspend fun toggleAlarm(alarm: AlarmEntity, enabled: Boolean) {
        val updatedAlarm = alarm.copy(isEnabled = enabled)
        Log.d("AppAlarmManager", "Alarm toggled: $updatedAlarm")
        repository.updateAlarm(updatedAlarm)
        if (enabled) {
            systemScheduler.scheduleAlarm(updatedAlarm, alarm.id)
        } else {
            systemScheduler.cancelAlarm(updatedAlarm)
        }
    }

    suspend fun deleteAlarm(alarm: AlarmEntity) {
        repository.deleteAlarm(alarm)
        systemScheduler.cancelAlarm(alarm)
    }

    suspend fun getAlarm(alarmId: Long): AlarmEntity? {
        return repository.getAlarmById(alarmId)
    }

    suspend fun scheduleNextAlarmOrDisable(alarmId: Long) {
        val alarm = repository.getAlarmById(alarmId) ?: return
        if (scheduleNextAlarm(alarm)) {
            Log.d(TAG, "Alarm scheduled: $alarm")
        } else {
            repository.updateAlarm(alarm.copy(isEnabled = false))
            Log.d(TAG, "No next alarm time: $alarm")
        }
    }

    fun scheduleNextAlarm(alarm: AlarmEntity) : Boolean {
        val nextAlarmTime = calculateNextAlarmTime(alarm)
        val remainingSnooze = if (alarm.snoozeEnabled) alarm.snoozeMaxCount!! else 0
        if (nextAlarmTime != -1L) {
            systemScheduler.scheduleNextAlarm(alarm, alarm.id, remainingSnooze, nextAlarmTime)
            return true
        } else {
            return false
        }
    }

    fun calculateNextAlarmTime(alarm: AlarmEntity): Long {
        val hour = alarm.hour
        val minute = alarm.minute
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        if (alarm.repeatDays.isEmpty()) {
            return -1
        }
        if (alarm.repeatDays.size == 7) {
            calendar.apply {
                add(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return calendar.timeInMillis
        }
        if (alarm.repeatDays.size == 1) {
            calendar.apply {
                add(Calendar.DAY_OF_MONTH, 7)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return calendar.timeInMillis
        }
        var dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        //TODO FORCE LOCALE OR DO STH BETTER
        dayOfWeek = (dayOfWeek + 5) % 7 + 1
        Log.d(TAG, "DEBUG: Day of week is $dayOfWeek")
        //TODO Call requires API level 26 (current min is 24): java.time.DayOfWeek#of
        var startIndex = helperDays.indexOf(DayOfWeek.of(dayOfWeek))
        var index = startIndex + 1
        var nextDay: DayOfWeek? = null
        while (index in helperDays.indices) {
            val day = helperDays[index]
            if (alarm.repeatDays.contains(day)) {
                nextDay = day
                break
            }
            index++
        }
        if (nextDay == null) {
            return -1
        }
        Log.d(TAG, "DEBUG: Next day is $nextDay" +
                "\nDEBUG: Index is $index" +
                "\nDEBUG: Start index is $startIndex"
        )
        calendar.apply {
            add(Calendar.DAY_OF_MONTH, index - startIndex)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    suspend fun scheduleAllEnabledAlarms() {
        repository.getAlarms().collect { alarms ->
            alarms.forEach { alarm ->
                if (alarm.isEnabled) {
                    val remainingSnooze = if (alarm.snoozeEnabled) alarm.snoozeMaxCount!! else 0
                    systemScheduler.scheduleAlarm(alarm, alarm.id, remainingSnooze)
                }
            }
        }
    }

    companion object {
        private const val TAG = "AppAlarmManager"
        private val days = DayOfWeek.values().toList()
        private val helperDays = days + days
    }
}
