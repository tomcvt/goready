package com.tomcvt.goready.data.seeding

import com.tomcvt.goready.data.StepDefinitionEntity
import kotlinx.serialization.Serializable

@Serializable
data class StepsJsonTemplate(
    val steps: List<StepDefinitionEntity>
)
