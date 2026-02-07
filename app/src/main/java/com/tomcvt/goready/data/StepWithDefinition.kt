package com.tomcvt.goready.data

data class StepWithDefinition(
    val id: Long,
    val routineId: Long,
    val stepId: Long,
    val stepNumber: Int,
    val length: Long,
    val stepType: String,
    val name: String,
    val description: String,
    val icon: String,
    val updatable: Boolean
)
