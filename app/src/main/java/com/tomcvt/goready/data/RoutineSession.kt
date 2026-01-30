package com.tomcvt.goready.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routine_sessions")
data class RoutineSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val routineId: Long,
    val stepNumber: Int,
    val stepStatus: StepStatus,
    val stepStartTime: Long,
    val maxSteps: Int,
    val status: RoutineStatus,
    val startTime: Long,
    val endTime: Long?
)