package com.tomcvt.goready.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tomcvt.goready.constants.StepType
import kotlinx.coroutines.flow.Flow

@Dao
interface StepDefinitionDao {
    @Query("SELECT * FROM step_definitions")
    fun getAllStepDefinitionsFlow(): Flow<List<StepDefinitionEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStepDefinition(stepDefinition: StepDefinitionEntity) : Long

    @Delete
    suspend fun deleteStepDefinition(stepDefinition: StepDefinitionEntity)

    @Update
    suspend fun updateStepDefinition(stepDefinition: StepDefinitionEntity)

    @Query("SELECT * FROM step_definitions WHERE id = :id")
    suspend fun getStepDefinitionById(id: Long) : StepDefinitionEntity?

    @Query("SELECT * FROM step_definitions WHERE stepType = :type")
    fun getStepDefinitionsByTypeFlow(type: StepType) : Flow<List<StepDefinitionEntity>>
}
