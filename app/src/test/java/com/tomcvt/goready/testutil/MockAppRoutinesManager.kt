package com.tomcvt.goready.testutil

import com.tomcvt.goready.constants.StepType
import com.tomcvt.goready.data.RoutineEntity
import com.tomcvt.goready.data.RoutineStepEntity
import com.tomcvt.goready.data.StepDefinitionEntity
import com.tomcvt.goready.data.StepWithDefinition
import com.tomcvt.goready.domain.RoutineDraft
import com.tomcvt.goready.domain.StepDefinitionDraft
import com.tomcvt.goready.manager.AppRoutinesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class MockAppRoutinesManager : AppRoutinesManager {
    override fun getAllRoutinesFlow(): Flow<List<RoutineEntity>> {
        //TODO("Not yet implemented")
        return emptyFlow()
    }

    override fun getRoutineByIdFlow(id: Long): Flow<RoutineEntity?> {
        //TODO("Not yet implemented")
        return emptyFlow()
    }

    override fun getUserStepDefinitionsFlow(): Flow<List<StepDefinitionEntity>> {
        //TODO("Not yet implemented")
        return emptyFlow()
    }

    override fun getAllStepDefinitionsFlow(): Flow<List<StepDefinitionEntity>> {
        //TODO("Not yet implemented")
        return emptyFlow()
    }

    override fun getRoutineStepsFlow(routineId: Long): Flow<List<RoutineStepEntity>> {
        //TODO("Not yet implemented")
        return emptyFlow()
    }

    override fun getRoutineStepsWithDefinitionFlow(routineId: Long): Flow<List<StepWithDefinition>> {
        //TODO("Not yet implemented")
        return emptyFlow()
    }

    override fun getStepDefinitionsByTypeFlow(type: StepType): Flow<List<StepDefinitionEntity>> {
        //TODO("Not yet implemented")
        return emptyFlow()
    }

    override suspend fun getStepDefinition(id: Long): StepDefinitionEntity? {
        //TODO("Not yet implemented")
        return null
    }

    override suspend fun getRoutineById(id: Long): RoutineEntity? {
        //TODO("Not yet implemented")
        return null
    }

    override suspend fun addStepDefinition(stepDefinitionDraft: StepDefinitionDraft): Long {
        //TODO("Not yet implemented")
        return 0L
    }

    override suspend fun updateStepDefinition(stepDefinitionDraft: StepDefinitionDraft) {
        //TODO("Not yet implemented")
    }

    override suspend fun addRoutine(routineDraft: RoutineDraft) {
        //TODO("Not yet implemented")
    }

    override suspend fun deleteRoutine(routine: RoutineEntity) {
        //TODO("Not yet implemented")
    }

}