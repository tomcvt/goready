package com.tomcvt.goready.testutil

import com.tomcvt.goready.data.AlarmEntity
import com.tomcvt.goready.domain.AlarmDraft
import com.tomcvt.goready.manager.AppAlarmManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class MockAppAlarmManager : AppAlarmManager {
    override fun getAlarmsFlow(): Flow<List<AlarmEntity>> {
        //TODO("Not yet implemented")
        return emptyFlow()
    }

    override suspend fun createAlarm(draft: AlarmDraft) {
        //TODO("Not yet implemented")
    }

    override fun scheduleSnoozeById(
        alarmId: Long,
        remainingSnooze: Int,
        snoozeTimeMinutes: Int
    ) {
        //TODO("Not yet implemented")
    }

    override suspend fun updateAlarm(
        draft: AlarmDraft,
        alarmId: Long
    ) {
        //TODO("Not yet implemented")
    }

    override suspend fun toggleAlarm(
        alarm: AlarmEntity,
        enabled: Boolean
    ) {
        //TODO("Not yet implemented")
    }

    override suspend fun deleteAlarm(alarm: AlarmEntity) {
        //TODO("Not yet implemented")
    }

    override suspend fun getAlarm(alarmId: Long): AlarmEntity? {
        //TODO("Not yet implemented")
        return null
    }

    override suspend fun scheduleNextAlarmOrDisable(alarmId: Long) {
        //TODO("Not yet implemented")
    }

    override suspend fun scheduleAllEnabledAlarms() {
        //TODO("Not yet implemented")
    }

}