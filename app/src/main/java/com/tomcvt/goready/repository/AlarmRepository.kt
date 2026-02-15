package com.tomcvt.goready.repository

import com.tomcvt.goready.data.AlarmEntity
import kotlinx.coroutines.flow.Flow

interface AlarmRepository {
    fun getAlarms(): Flow<List<AlarmEntity>>
    suspend fun insertAlarm(alarm: AlarmEntity) : Long
    suspend fun deleteAlarm(alarm: AlarmEntity)
    suspend fun updateAlarm(alarm: AlarmEntity)
    suspend fun getAlarmById(id: Long): AlarmEntity?
}