package com.tomcvt.goready.repository

import com.tomcvt.goready.data.AlarmDao
import com.tomcvt.goready.data.AlarmEntity
import kotlinx.coroutines.flow.Flow

class AlarmRepository(private val dao: AlarmDao) {
    fun getAlarms(): Flow<List<AlarmEntity>> = dao.getAlarms()

    suspend fun insertAlarm(alarm: AlarmEntity) {
        dao.insertAlarm(alarm)
    }

    suspend fun deleteAlarm(alarm: AlarmEntity) {
        dao.deleteAlarm(alarm)
    }

    suspend fun updateAlarm(alarm: AlarmEntity) {
        dao.updateAlarm(alarm)
    }
}
