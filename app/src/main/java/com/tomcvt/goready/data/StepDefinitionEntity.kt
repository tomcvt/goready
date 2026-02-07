package com.tomcvt.goready.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "step_definitions")
data class StepDefinitionEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val stepType: String,
    val name: String,
    val description: String,
    val icon: String,
    val updatable: Boolean = false
)