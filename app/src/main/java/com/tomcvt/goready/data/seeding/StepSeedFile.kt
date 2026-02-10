package com.tomcvt.goready.data.seeding

import com.tomcvt.goready.data.StepDefinitionEntity

data class StepSeedFile(
    val version: Int,
    val replace: Boolean = false,
    val stepDefinitions: List<StepDefinitionEntity>
)
