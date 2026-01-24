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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineStep(routineStep: RoutineStepEntity)

    @Query("DELETE FROM routine_steps WHERE id = :id")
    suspend fun deleteRoutineStep(id: Long)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateRoutineStep(routineStep: RoutineStepEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateRoutineSteps(routineSteps: List<RoutineStepEntity>)



}