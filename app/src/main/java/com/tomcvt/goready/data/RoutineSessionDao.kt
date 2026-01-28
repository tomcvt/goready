package com.tomcvt.goready.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineSessionDao {

    @Insert
    suspend fun insertRoutineSession(routineSession: RoutineSession): Long

    @Query("SELECT * FROM routine_sessions WHERE id = :id")
    fun getRoutineSessionByIdFlow(id: Long): Flow<RoutineSession?>

    @Update
    suspend fun updateRoutineSession(routineSession: RoutineSession)

    @Query("SELECT * FROM routine_sessions WHERE status = :status")
    fun getRoutineSessionsByStatusFlow(status: RoutineStatus): Flow<List<RoutineSession>>

    @Update
    suspend fun updateRoutineSessions(routineSessions: List<RoutineSession>)

}