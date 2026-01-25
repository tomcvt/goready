package com.tomcvt.goready.manager

import com.tomcvt.goready.data.StepDefinitionEntity
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

    suspend fun addStepDefinition(stepDefinitionDraft: StepDefinitionDraft) : Long {
        val stepDefinitionEntity = stepDefinitionDraft.toEntity()
        return stepDefinitionRepository.insertStepDefinition(stepDefinitionEntity)
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
