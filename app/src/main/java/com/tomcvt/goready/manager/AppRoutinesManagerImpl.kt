package com.tomcvt.goready.manager

import android.util.Log
import com.tomcvt.goready.constants.StepType
import com.tomcvt.goready.data.RoutineEntity
import com.tomcvt.goready.data.RoutineStepEntity
import com.tomcvt.goready.data.StepDefinitionEntity
import com.tomcvt.goready.domain.OpResult
import com.tomcvt.goready.domain.RoutineDraft
import com.tomcvt.goready.domain.StepDefinitionDraft
import com.tomcvt.goready.repository.RoutineRepository
import com.tomcvt.goready.repository.RoutineStepRepository
import com.tomcvt.goready.repository.StepDefinitionRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AppRoutinesManagerImpl(
    private val routineRepository: RoutineRepository,
    private val routineStepRepository: RoutineStepRepository,
    private val stepDefinitionRepository: StepDefinitionRepository
) : AppRoutinesManager {
    override fun getAllRoutinesFlow() = routineRepository.getAllRoutinesFlow()

    override fun getRoutineByIdFlow(id: Long) = routineRepository.getRoutineByIdFlow(id)

    override fun getUserStepDefinitionsFlow() = stepDefinitionRepository.getUserStepDefinitionsFlow()

    override fun getAllStepDefinitionsFlow() = stepDefinitionRepository.getAllStepDefinitionsFlow()

    override fun getRoutineStepsFlow(routineId: Long) = routineStepRepository.getRoutineStepsFlow(routineId)

    override fun getRoutineStepsWithDefinitionFlow(routineId: Long) = routineStepRepository.getRoutineStepsWithDefinitionFlow(routineId)

    override fun getStepDefinitionsByTypeFlow(type: StepType) = stepDefinitionRepository.getStepDefinitionsByTypeFlow(type)

    override suspend fun getStepDefinition(id: Long) = stepDefinitionRepository.getStepDefinition(id)

    override suspend fun getRoutineById(id: Long) = routineRepository.getRoutineById(id)

    override suspend fun addStepDefinition(stepDefinitionDraft: StepDefinitionDraft) : Long {
        val stepDefinitionEntity = stepDefinitionDraft.toEntity()
        val jsonString = Json.encodeToString<StepDefinitionEntity>(value = stepDefinitionEntity)
        Log.d("JSON_DEV", jsonString)
        return stepDefinitionRepository.insertStepDefinition(stepDefinitionEntity)
    }

    override suspend fun updateStepDefinition(stepDefinitionDraft: StepDefinitionDraft) {
        val stepDefinitionEntity = stepDefinitionDraft.toEntityUpdate()
        val jsonString = Json.encodeToString<StepDefinitionEntity>(stepDefinitionEntity)
        Log.d("JSON_DEV", jsonString)
        stepDefinitionRepository.updateStepDefinition(stepDefinitionEntity)
    }

    override suspend fun deleteStepDefinition(stepDefinition: StepDefinitionEntity) : OpResult<Unit> {
        if (stepDefinition.seedKey != null) {
            Log.e(TAG, "Cannot delete seeded definition")
            return OpResult.Error(Exception("This is a built in feature, cannot be deleted"))
        }
        try {
            stepDefinitionRepository.deleteStepDefinition(stepDefinition)
        } catch (e: Exception) {
            if (e.message?.contains("FOREIGN KEY constraint failed") == true) {
                Log.e(TAG, "Cannot delete definition with steps")
                return OpResult.Error(Exception("This step is used in a routine, cannot be deleted"))
            }
            return OpResult.Error(e)
        }
        return OpResult.Success(Unit)

    }


    //Add/edit routine by id

    override suspend fun addRoutine(routineDraft: RoutineDraft) {
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

    override suspend fun deleteRoutine(routine: RoutineEntity) {
        routineRepository.deleteRoutine(routine)
        routineStepRepository.deleteRoutineStepsForRoutine(routine.id)
    }

    companion object {
        private const val TAG = "AppRoutinesManagerImpl"
    }
}

private fun StepDefinitionDraft.toEntity() : StepDefinitionEntity {
    return StepDefinitionEntity(
        stepType = this.stepType,
        name = this.name,
        description = this.description,
        icon = this.icon,
        suggestedTimeMinutes = this.suggestedTimeMinutes
    )
}

private fun StepDefinitionDraft.toEntityUpdate() : StepDefinitionEntity {
    return StepDefinitionEntity(
        id = this.id,
        stepType = this.stepType,
        name = this.name,
        description = this.description,
        icon = this.icon,
        suggestedTimeMinutes = this.suggestedTimeMinutes
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

