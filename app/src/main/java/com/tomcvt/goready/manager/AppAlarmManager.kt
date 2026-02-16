package com.tomcvt.goready.manager

import android.util.Log
import com.tomcvt.goready.data.AlarmEntity
import com.tomcvt.goready.domain.AlarmDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Date

interface AppAlarmManager {
    fun getAlarmsFlow() : Flow<List<AlarmEntity>>
    suspend fun createAlarm(draft: AlarmDraft)

    fun scheduleSnoozeById(alarmId: Long, remainingSnooze: Int, snoozeTimeMinutes: Int)

    suspend fun updateAlarm(draft: AlarmDraft, alarmId: Long)

    suspend fun toggleAlarm(alarm: AlarmEntity, enabled: Boolean)

    suspend fun deleteAlarm(alarm: AlarmEntity)

    suspend fun getAlarm(alarmId: Long): AlarmEntity?

    suspend fun scheduleNextAlarmOrDisable(alarmId: Long)

    suspend fun scheduleAllEnabledAlarms()
}