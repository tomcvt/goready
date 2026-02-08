package com.tomcvt.goready.domain

import com.tomcvt.goready.constants.StepType

data class StepDefinitionDraft (
    val id: Long,
    val stepType: StepType,
    val name: String,
    val description: String,
    val icon: String,
)