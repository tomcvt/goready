package com.tomcvt.goready.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tomcvt.goready.constants.StepType
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "step_definitions", indices = [
    Index(value = ["seedKey"], unique = true)
])
data class StepDefinitionEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val seedKey: String? = null,
    val stepType: StepType,
    val name: String,
    val description: String,
    val icon: String,
    val updatable: Boolean = false
)