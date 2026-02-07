package com.tomcvt.goready.domain

data class StepDefinitionDraft (
    val id: Long,
    val stepType: String,
    val name: String,
    val description: String,
    val icon: String,
)