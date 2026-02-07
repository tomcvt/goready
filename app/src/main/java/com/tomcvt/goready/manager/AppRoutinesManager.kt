package com.tomcvt.goready.manager

import androidx.room.Transaction
import com.tomcvt.goready.data.RoutineEntity
import com.tomcvt.goready.data.RoutineStepEntity
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

    fun getRoutineByIdFlow(id: Long) = routineRepository.getRoutineByIdFlow(id)


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

    suspend fun updateStepDefinition(stepDefinitionDraft: StepDefinitionDraft) {
        val stepDefinitionEntity = stepDefinitionDraft.toEntityUpdate()
        stepDefinitionRepository.updateStepDefinition(stepDefinitionEntity)
    }

    //Add/edit routine by id

    suspend fun addRoutine(routineDraft: RoutineDraft) {
        if (routineDraft.id == null) {
            val routineEntity = routineDraft.toEntity()
            val routineId = routineRepository.insertRoutine(routineEntity)

            //TODO add steps
            for (i in routineDraft.steps.indices) {
                val step = routineDraft.steps[i]
                val routineStepEntity = RoutineStepEntity(
                    routineId = routineId,
                    stepId = step.first.id,
                    stepNumber = i,
                    length = step.second.toLong()
                )
                //can get i here but for what?
                routineStepRepository.insertRoutineStep(routineStepEntity)
            }
        } else {
            val routineEntity = routineDraft.toEntity()
            routineRepository.updateRoutine(routineEntity)
            val routineId = routineEntity.id
            val list = mutableListOf<RoutineStepEntity>()
            for (i in routineDraft.steps.indices) {
                val step = routineDraft.steps[i]
                val routineStepEntity = RoutineStepEntity(
                    routineId = routineId,
                    stepId = step.first.id,
                    stepNumber = i,
                    length = step.second.toLong()
                )
                list.add(routineStepEntity)
            }
            routineStepRepository.replaceRoutineSteps(routineId, list)
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
        icon = this.icon,
        updatable = true
    )
}

private fun StepDefinitionDraft.toEntityUpdate() : StepDefinitionEntity {
    return StepDefinitionEntity(
        id = this.id,
        stepType = this.stepType,
        name = this.name,
        description = this.description,
        icon = this.icon,
        updatable = true
    )
}

private fun RoutineDraft.toEntity() : RoutineEntity {
    return RoutineEntity(
        id = this.id?: 0,
        name = this.name,
        description = this.description,
        icon = this.icon
    )
}

