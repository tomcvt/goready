package com.tomcvt.goready.repository

import com.tomcvt.goready.data.AlarmDao
import com.tomcvt.goready.data.AlarmEntity
import kotlinx.coroutines.flow.Flow

class AlarmRepositoryImpl(private val dao: AlarmDao) : AlarmRepository {
    override fun getAlarms(): Flow<List<AlarmEntity>> = dao.getAlarms()

    override suspend fun insertAlarm(alarm: AlarmEntity) : Long {
        return dao.insertAlarm(alarm)
    }

    override suspend fun deleteAlarm(alarm: AlarmEntity) {
        dao.deleteAlarm(alarm)
    }

    override suspend fun updateAlarm(alarm: AlarmEntity) {
        dao.updateAlarm(alarm)
    }

    override suspend fun getAlarmById(id: Long): AlarmEntity? {
        return dao.getAlarmById(id)
    }
}
