package com.tomcvt.goready.data

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "routine_steps")
class RoutineStepEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val routineId: Long,
    val stepNumber: Int,
    val stepType: String,
    val name: String,
    val description: String,
    val length: Long,
    val icon: String,

    )