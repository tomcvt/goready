package com.tomcvt.goready.testutil

import com.tomcvt.goready.data.AlarmEntity
import com.tomcvt.goready.repository.AlarmRepository
import kotlinx.coroutines.flow.flowOf

class FakeAlarmRepository : AlarmRepository {

    private val alarms = mutableMapOf<Long, AlarmEntity>()
    private var idCounter = 1L

    override suspend fun insertAlarm(alarm: AlarmEntity): Long {
        val id = idCounter++
        alarms[id] = alarm.copy(id = id)
        return id
    }

    override suspend fun updateAlarm(alarm: AlarmEntity) {
        alarms[alarm.id] = alarm
    }

    override suspend fun deleteAlarm(alarm: AlarmEntity) {
        alarms.remove(alarm.id)
    }

    override suspend fun getAlarmById(id: Long): AlarmEntity? {
        return alarms[id]
    }

    override fun getAlarms() = flowOf(alarms.values.toList())
}