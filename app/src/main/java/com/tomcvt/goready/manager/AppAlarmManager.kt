package com.tomcvt.goready.manager

import android.util.Log
import com.tomcvt.goready.data.AlarmEntity
import com.tomcvt.goready.domain.AlarmDraft
import com.tomcvt.goready.repository.AlarmRepository
import com.tomcvt.goready.repository.AlarmRepositoryImpl
import com.tomcvt.goready.time.RealTimeProvider
import com.tomcvt.goready.time.RepeatAlarmCalculator
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.util.Date

open class AppAlarmManager(
    private val repository: AlarmRepository,
    private val systemScheduler: AlarmScheduler,
    private val repeatAlarmCalculator: RepeatAlarmCalculator = RepeatAlarmCalculator(
        RealTimeProvider()
    )
) {
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
        val newAlarmId = repository.insertAlarm(entity)
        scheduleOrCancelBasedOnRepeatDays(entity.copy(id = newAlarmId))
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

        systemScheduler.cancelAlarm(oldAlarm)
        scheduleOrCancelBasedOnRepeatDays(updatedAlarm)

        repository.updateAlarm(updatedAlarm)
    }


    suspend fun toggleAlarm(alarm: AlarmEntity, enabled: Boolean) {
        val updatedAlarm = alarm.copy(isEnabled = enabled)
        Log.d("AppAlarmManager", "Alarm toggled: $updatedAlarm")
        repository.updateAlarm(updatedAlarm)
        scheduleOrCancelBasedOnRepeatDays(updatedAlarm)
    }

    private fun scheduleBasedOnRepeatDays(alarm: AlarmEntity) {
        val snoozeCount = if (alarm.snoozeEnabled) alarm.snoozeMaxCount!! else 0
        if (alarm.repeatDays.isEmpty()) {
            systemScheduler.scheduleOneTimeAlarm(alarm, alarm.id, snoozeCount)
            return
        } else {
            val nextAlarmTime = repeatAlarmCalculator.calculateNextAlarmTime(alarm)
            systemScheduler.scheduleNextAlarm(alarm, alarm.id, snoozeCount, nextAlarmTime)
        }
    }

    private fun scheduleOrCancelBasedOnRepeatDays(alarm: AlarmEntity) {
        val enabled = alarm.isEnabled
        if (enabled) {
            scheduleBasedOnRepeatDays(alarm)
        } else {
            systemScheduler.cancelAlarm(alarm)
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
        val nextAlarmTime = repeatAlarmCalculator.calculateNextAlarmTime(alarm)
        val remainingSnooze = if (alarm.snoozeEnabled) alarm.snoozeMaxCount!! else 0
        if (nextAlarmTime != -1L) {
            systemScheduler.scheduleNextAlarm(alarm, alarm.id, remainingSnooze, nextAlarmTime)
            Log.d(TAG, "Next alarm time: ${Date(nextAlarmTime)}")
        } else {
            repository.updateAlarm(alarm.copy(isEnabled = false))
            Log.d(TAG, "No next alarm time: $alarm")
        }
    }

    suspend fun scheduleAllEnabledAlarms() {
        repository.getAlarms().first().forEach { alarm ->
            scheduleBasedOnRepeatDays(alarm)
        }
    }

    companion object {
        private const val TAG = "AppAlarmManager"
        private val days = DayOfWeek.values().toList()
        private val helperDays = days + days
    }
}
