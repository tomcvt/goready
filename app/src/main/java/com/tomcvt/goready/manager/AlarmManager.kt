package com.tomcvt.goready.manager

import android.util.Log
import com.tomcvt.goready.data.AlarmEntity
import com.tomcvt.goready.domain.AlarmDraft
import com.tomcvt.goready.domain.SimpleAlarmDraft
import com.tomcvt.goready.repository.AlarmRepository
import java.time.DayOfWeek

open class AlarmManager(private val repository: AlarmRepository, private val systemScheduler: SystemAlarmScheduler) {

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
            snoozeMaxCount = draft.snoozeMaxCount
        )

        // 2. Save to DB
        val newAlarmId = repository.insertAlarm(entity)

        // 3. Schedule system alarm
        systemScheduler.scheduleAlarm(entity, newAlarmId)
    }

    suspend fun toggleAlarm(alarm: AlarmEntity, enabled: Boolean) {
        val updatedAlarm = alarm.copy(isEnabled = enabled)
        Log.d("AlarmManager", "Alarm toggled: $updatedAlarm")
        repository.updateAlarm(updatedAlarm)
        if (enabled) {
            systemScheduler.scheduleAlarm(updatedAlarm, alarm.id)
        } else {
            systemScheduler.cancelAlarm(updatedAlarm)
        }
    }

    suspend fun createSimpleAlarm(draft: SimpleAlarmDraft) {
        // 1. Convert draft → entity
        val entity = AlarmEntity(
            hour = draft.hour,
            minute = draft.minute,
            label = "Alarm",
            repeatDays = draft.repeatDays,
            isEnabled = true,
            task = "None",
            taskData = null,
            soundUri = null,
            snoozeEnabled = false,
            snoozeDurationMinutes = null,
            snoozeMaxCount = null
        )

        // 2. Save to DB
        val newAlarmId = repository.insertAlarm(entity)

        // 3. Schedule system alarm
        systemScheduler.scheduleAlarm(entity, newAlarmId)
    }

    suspend fun deleteAlarm(alarm: AlarmEntity) {
        repository.deleteAlarm(alarm)
        systemScheduler.cancelAlarm(alarm)

    }

}