package com.tomcvt.goready.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow


@Dao
interface RoutineStepDao {
    @Query("SELECT * FROM routine_steps WHERE routineId = :routineId ORDER BY stepNumber ASC")
    fun getRoutineStepsFlow(routineId: Long): Flow<List<RoutineStepEntity>>

    @Query("""
        SELECT
            rs.id,
            rs.routineId,
            rs.stepId,
            rs.stepNumber,
            rs.length,
            sd.stepType,
            sd.name,
            sd.description,
            sd.icon
        FROM routine_steps rs
        JOIN step_definitions sd ON rs.stepId = sd.id
        WHERE rs.routineId = :routineId
        ORDER BY rs.stepNumber ASC
    """)
    fun getRoutineStepsWithDefinitionFlow(routineId: Long): Flow<List<StepWithDefinition>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineStep(routineStep: RoutineStepEntity) : Long

    @Deprecated("Use by id instead")
    @Query("DELETE FROM routine_steps WHERE routineId = :routineId AND stepId = :stepId")
    suspend fun deleteRoutineStepDuplicated(routineId: Long, stepId: Long)

    @Query("DELETE FROM routine_steps WHERE id = :id")
    suspend fun deleteRoutineStepById(id: Long)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateRoutineStep(routineStep: RoutineStepEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateRoutineSteps(routineSteps: List<RoutineStepEntity>)

}