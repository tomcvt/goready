package com.tomcvt.goready.repository

import com.tomcvt.goready.constants.StepType
import com.tomcvt.goready.data.StepDefinitionDao
import com.tomcvt.goready.data.StepDefinitionEntity
import kotlinx.coroutines.flow.Flow

class StepDefinitionRepository(
    private val stepDefinitionDao: StepDefinitionDao
) {
    fun getAllStepDefinitionsFlow(): Flow<List<StepDefinitionEntity>> {
        return stepDefinitionDao.getAllStepDefinitionsFlow()
    }

    fun getStepDefinitionsByTypeFlow(type: StepType): Flow<List<StepDefinitionEntity>> {
        return stepDefinitionDao.getStepDefinitionsByTypeFlow(type)
    }

    fun getUserStepDefinitionsFlow(): Flow<List<StepDefinitionEntity>> {
        return stepDefinitionDao.getUserStepDefinitionsFlow()
    }

    suspend fun getStepDefinition(id: Long) : StepDefinitionEntity? {
        return stepDefinitionDao.getStepDefinitionById(id)
    }

    suspend fun insertStepDefinition(stepDefinitionEntity: StepDefinitionEntity) : Long {
        return stepDefinitionDao.insertStepDefinition(stepDefinitionEntity)
    }

    suspend fun updateStepDefinition(stepDefinitionEntity: StepDefinitionEntity) {
        stepDefinitionDao.updateStepDefinition(stepDefinitionEntity)
    }
}