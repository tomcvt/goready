package com.tomcvt.goready.repository

import com.tomcvt.goready.data.RoutineDao
import com.tomcvt.goready.data.RoutineEntity
import com.tomcvt.goready.data.RoutineStepDao
import com.tomcvt.goready.data.RoutineStepEntity
import com.tomcvt.goready.data.StepDefinitionDao
import com.tomcvt.goready.data.StepWithDefinition
import kotlinx.coroutines.flow.Flow

class RoutineRepository(
    private val routineDao: RoutineDao
) {
    fun getAllRoutinesFlow(): Flow<List<RoutineEntity>> {
        return routineDao.getAllRoutinesFlow()
    }

    suspend fun insertRoutine(routine: RoutineEntity): Long {
        return routineDao.insertRoutine(routine)
    }

    suspend fun getRoutineById(id: Long): RoutineEntity {
        return routineDao.getRoutineById(id)
    }

    suspend fun deleteRoutine(routine: RoutineEntity) {
        routineDao.deleteRoutine(routine)
    }

    suspend fun updateRoutine(routine: RoutineEntity) {
        routineDao.updateRoutine(routine)
    }
    // maybe later add methods for whole routine

}