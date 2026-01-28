package com.tomcvt.goready.repository

import com.tomcvt.goready.data.RoutineSession
import com.tomcvt.goready.data.RoutineSessionDao
import com.tomcvt.goready.data.RoutineStatus

data class RoutineSessionRepository(
    private val routineSessionDao: RoutineSessionDao
) {
    fun getRoutineSessionByIdFlow(id: Long) = routineSessionDao.getRoutineSessionByIdFlow(id)

    suspend fun insertRoutineSession(routineSession: RoutineSession) : Long {
        return routineSessionDao.insertRoutineSession(routineSession)
    }

    suspend fun updateRoutineSession(routineSession: RoutineSession) {
        routineSessionDao.updateRoutineSession(routineSession)
    }

    fun getRoutineSessionsByStatusFlow(status: RoutineStatus) = routineSessionDao.getRoutineSessionsByStatusFlow(status)

    suspend fun updateRoutineSessions(routineSessions: List<RoutineSession>) {
        routineSessionDao.updateRoutineSessions(routineSessions)
    }
}
