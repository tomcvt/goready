package com.tomcvt.goready.repository

import com.tomcvt.goready.data.StepDefinitionDao
import com.tomcvt.goready.data.StepDefinitionEntity
import kotlinx.coroutines.flow.Flow

class StepDefinitionRepository(
    private val stepDefinitionDao: StepDefinitionDao
) {
    fun getAllStepDefinitionsFlow(): Flow<List<StepDefinitionEntity>> {
        return stepDefinitionDao.getAllStepDefinitionsFlow()
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