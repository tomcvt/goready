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

    suspend fun insertStepDefinition(stepDefinitionEntity: StepDefinitionEntity) : Long {
        return stepDefinitionDao.insertStepDefinition(stepDefinitionEntity)
    }
}