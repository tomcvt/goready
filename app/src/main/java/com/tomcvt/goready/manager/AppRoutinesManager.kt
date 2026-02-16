package com.tomcvt.goready.manager

import android.util.Log
import com.tomcvt.goready.constants.StepType
import com.tomcvt.goready.data.RoutineEntity
import com.tomcvt.goready.data.RoutineStepEntity
import com.tomcvt.goready.data.StepDefinitionEntity
import com.tomcvt.goready.data.StepWithDefinition
import com.tomcvt.goready.domain.RoutineDraft
import com.tomcvt.goready.domain.StepDefinitionDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface AppRoutinesManager {
    fun getAllRoutinesFlow() : Flow<List<RoutineEntity>>

    fun getRoutineByIdFlow(id: Long) : Flow<RoutineEntity?>

    fun getUserStepDefinitionsFlow() : Flow<List<StepDefinitionEntity>>


    fun getAllStepDefinitionsFlow() : Flow<List<StepDefinitionEntity>>


    fun getRoutineStepsFlow(routineId: Long) : Flow<List<RoutineStepEntity>>

    fun getRoutineStepsWithDefinitionFlow(routineId: Long) : Flow<List<StepWithDefinition>>


    fun getStepDefinitionsByTypeFlow(type: StepType) : Flow<List<StepDefinitionEntity>>

    suspend fun getStepDefinition(id: Long) : StepDefinitionEntity?

    suspend fun getRoutineById(id: Long) : RoutineEntity?


    suspend fun addStepDefinition(stepDefinitionDraft: StepDefinitionDraft) : Long

    suspend fun updateStepDefinition(stepDefinitionDraft: StepDefinitionDraft)

    suspend fun addRoutine(routineDraft: RoutineDraft)

    suspend fun deleteRoutine(routine: RoutineEntity)
}