package com.tomcvt.goready.manager

import com.tomcvt.goready.data.RoutineSession
import com.tomcvt.goready.data.RoutineStatus
import com.tomcvt.goready.data.StepStatus
import com.tomcvt.goready.repository.RoutineRepository
import com.tomcvt.goready.repository.RoutineSessionRepository
import com.tomcvt.goready.repository.RoutineStepRepository
import com.tomcvt.goready.repository.StepDefinitionRepository
import kotlinx.coroutines.flow.first

class RoutineFlowManager(
    private val routineRepository: RoutineRepository,
    private val routineStepRepository: RoutineStepRepository,
    private val stepDefinitionRepository: StepDefinitionRepository,
    private val routineSessionRepository: RoutineSessionRepository
) {

    suspend fun startRoutine(routineId: Long) {
        clearRunningRoutines()
        val routine = routineRepository.getRoutineById(routineId)
        val steps = routineStepRepository.getRoutineStepsWithDefinitionFlow(routineId).first()
        val session = RoutineSession(
            routineId = routineId,
            stepNumber = 0,
            stepStatus = StepStatus.RUNNING,
            stepStartTime = System.currentTimeMillis(),
            status = RoutineStatus.RUNNING,
            startTime = System.currentTimeMillis(),
            endTime = null //TODO calculate end time
        )

    }

    suspend fun clearRunningRoutines() {
        val sessions = routineSessionRepository.getRoutineSessionsByStatusFlow(RoutineStatus.RUNNING).first()
        sessions.forEach { session ->
            routineSessionRepository.updateRoutineSession(session.copy(status = RoutineStatus.CANCELED))
        }
    }
}