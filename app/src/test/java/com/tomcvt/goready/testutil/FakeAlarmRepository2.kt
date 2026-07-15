package com.tomcvt.goready.testutil

import com.tomcvt.goready.data.AlarmEntity
import com.tomcvt.goready.repository.AlarmRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeAlarmRepository2 : AlarmRepository {
    private val alarms : MutableStateFlow<List<AlarmEntity>> = MutableStateFlow(emptyList())
    private var idCounter : Long = 1L

    override suspend fun deleteAlarm(alarm: AlarmEntity) {
        alarms.value = alarms.value.filter { it.id != alarm.id }
    }

    override suspend fun getAlarmById(id: Long): AlarmEntity? {
        return alarms.value.find { it.id == id }
    }

    override suspend fun insertAlarm(alarm: AlarmEntity): Long {
        val newAlarm = alarm.copy(id = idCounter++)
        alarms.value += newAlarm
        return newAlarm.id
    }

    override suspend fun updateAlarm(alarm: AlarmEntity) {
        alarms.value = alarms.value.map { if (it.id == alarm.id) alarm else it }
    }

    override fun getAlarms(): StateFlow<List<AlarmEntity>> {
        return alarms
    }
}