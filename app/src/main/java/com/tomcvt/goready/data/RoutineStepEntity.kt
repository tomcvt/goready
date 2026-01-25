package com.tomcvt.goready.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(tableName = "routine_steps",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = CASCADE
        ),
        ForeignKey(
            entity = StepDefinitionEntity::class,
            parentColumns = ["id"],
            childColumns = ["stepId"],
            onDelete = CASCADE
        )
    ],
    indices = [
        Index("routineId"),
        Index("stepId")
    ]
    )
data class RoutineStepEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val routineId: Long,
    val stepId: Long,
    val stepNumber: Int,
    val length: Long,
    )