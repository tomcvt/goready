package com.tomcvt.goready.domain

import com.tomcvt.goready.data.StepDefinitionEntity

data class RoutineDraft(
    val id: Long?,
    val name: String,
    val description: String,
    val icon: String,
    val steps: List<Pair<StepDefinitionEntity, Int>>
)
