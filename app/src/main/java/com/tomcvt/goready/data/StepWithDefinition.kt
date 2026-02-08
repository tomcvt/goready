package com.tomcvt.goready.data

import com.tomcvt.goready.constants.StepType

data class StepWithDefinition(
    val id: Long,
    val routineId: Long,
    val stepId: Long,
    val stepNumber: Int,
    val length: Long,
    val stepType: StepType,
    val name: String,
    val description: String,
    val icon: String,
    val updatable: Boolean
)
