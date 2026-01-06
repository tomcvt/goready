package com.tomcvt.goready.manager

import com.tomcvt.goready.DayOfWeek
import com.tomcvt.goready.data.AlarmEntity
import com.tomcvt.goready.domain.AlarmDraft
import com.tomcvt.goready.repository.AlarmRepository

class AlarmManager(private val repository: AlarmRepository, private val systemScheduler: SystemAlarmScheduler) {
    suspend fun createAlarm(draft: AlarmDraft) {
        // 1. Convert draft → entity
        val entity = AlarmEntity(
            hour = draft.hour,
            minute = draft.minute,
            label = draft.label,
            repeatDays = convertDaysOfWeekSet(draft.repeatDays),
            enabled = true
        )

        // 2. Save to DB
        repository.insertAlarm(entity)

        // 3. Schedule system alarm
        systemScheduler.scheduleAlarm(entity)
    }

    suspend fun createSimpleAlarm(draft: AlarmDraft) {
        // 1. Convert draft → entity
        val entity = AlarmEntity(
            hour = draft.hour,
            minute = draft.minute,
            label = draft.label,
            repeatDays = convertDaysOfWeekSet(draft.repeatDays),
            isEnabled = true,
            task = "None",
            taskData = null,
            soundUri = draft.soundUri,
            snoozeEnabled = draft.snoozeEnabled,
            snoozeDurationMinutes = draft.snoozeDurationMinutes,
            snoozeMaxCount = draft.snoozeMaxCount
        )

        // 2. Save to DB
        repository.insertAlarm(entity)

        // 3. Schedule system alarm
        systemScheduler.scheduleAlarm(entity)
    }


    fun convertDaysOfWeekSet(repeatDays: Set<DayOfWeek>) : Set<java.time.DayOfWeek> {
        return repeatDays.map { day ->
            when (day) {
                DayOfWeek.MON -> java.time.DayOfWeek.MONDAY
                DayOfWeek.TUE -> java.time.DayOfWeek.TUESDAY
                DayOfWeek.WED -> java.time.DayOfWeek.WEDNESDAY
                DayOfWeek.THU -> java.time.DayOfWeek.THURSDAY
                DayOfWeek.FRI -> java.time.DayOfWeek.FRIDAY
                DayOfWeek.SAT -> java.time.DayOfWeek.SATURDAY
                DayOfWeek.SUN -> java.time.DayOfWeek.SUNDAY
            }
        }.toSet()
    }
}