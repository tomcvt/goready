package com.tomcvt.goready.repository

import com.tomcvt.goready.data.RoutineStepDao
import com.tomcvt.goready.data.RoutineStepEntity
import com.tomcvt.goready.data.StepWithDefinition
import kotlinx.coroutines.flow.Flow

class RoutineStepRepository(
    private val routineStepDao: RoutineStepDao
) {
    fun getRoutineStepsFlow(routineId: Long): Flow<List<RoutineStepEntity>> {
        return routineStepDao.getRoutineStepsFlow(routineId)
    }

    fun getRoutineStepsWithDefinitionFlow(routineId: Long): Flow<List<StepWithDefinition>> {
        return routineStepDao.getRoutineStepsWithDefinitionFlow(routineId)
    }

    suspend fun insertRoutineStep(routineStep: RoutineStepEntity) : Long {
        return routineStepDao.insertRoutineStep(routineStep)
    }

    suspend fun deleteRoutineStepsForRoutine(routineId: Long) {
        routineStepDao.deleteRoutineStepsForRoutine(routineId)
    }
}