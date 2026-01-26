package com.tomcvt.goready.manager

import com.tomcvt.goready.data.RoutineEntity
import com.tomcvt.goready.data.StepDefinitionEntity
import com.tomcvt.goready.domain.RoutineDraft
import com.tomcvt.goready.domain.StepDefinitionDraft
import com.tomcvt.goready.repository.RoutineRepository
import com.tomcvt.goready.repository.RoutineStepRepository
import com.tomcvt.goready.repository.StepDefinitionRepository

class AppRoutinesManager(
    private val routineRepository: RoutineRepository,
    private val routineStepRepository: RoutineStepRepository,
    private val stepDefinitionRepository: StepDefinitionRepository
) {
    fun getAllRoutinesFlow() = routineRepository.getAllRoutinesFlow()

    fun getAllStepDefinitionsFlow() = stepDefinitionRepository.getAllStepDefinitionsFlow()

    fun getRoutineStepsFlow(routineId: Long) = routineStepRepository.getRoutineStepsFlow(routineId)

    fun getRoutineStepsWithDefinitionFlow(routineId: Long) = routineStepRepository.getRoutineStepsWithDefinitionFlow(routineId)

    suspend fun getStepDefinition(id: Long) = stepDefinitionRepository.getStepDefinition(id)

    //fun getRoutineStep(id: Long) = routineStepRepository.getRoutineStep(id)
    suspend fun getRoutineById(id: Long) = routineRepository.getRoutineById(id)

    suspend fun addStepDefinition(stepDefinitionDraft: StepDefinitionDraft) : Long {
        val stepDefinitionEntity = stepDefinitionDraft.toEntity()
        return stepDefinitionRepository.insertStepDefinition(stepDefinitionEntity)
    }

    suspend fun addRoutine(routineDraft: RoutineDraft) {
        val routineEntity = routineDraft.toEntity()
        val routineId = routineRepository.insertRoutine(routineEntity)

        //TODO add steps
        for (step in routineDraft.steps) {
            val routineStepEntity = com.tomcvt.goready.data.RoutineStepEntity(
                routineId = routineId,
                stepId = step.first.id,
                stepNumber = 0,
                length = step.second.toLong()
            )
            //can get i here but for what?
            routineStepRepository.insertRoutineStep(routineStepEntity)
        }
    }

    suspend fun deleteRoutine(routine: RoutineEntity) {
        routineRepository.deleteRoutine(routine)
        routineStepRepository.deleteRoutineStepsForRoutine(routine.id)
    }
}

private fun StepDefinitionDraft.toEntity() : StepDefinitionEntity {
    return StepDefinitionEntity(
        stepType = this.stepType,
        name = this.name,
        description = this.description,
        icon = this.icon
    )
}

private fun RoutineDraft.toEntity() : RoutineEntity {
    return RoutineEntity(
        name = this.name,
        description = this.description,
        icon = this.icon
    )
}
