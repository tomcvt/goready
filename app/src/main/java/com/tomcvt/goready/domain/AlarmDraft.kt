package com.tomcvt.goready.domain

import java.time.DayOfWeek

data class AlarmDraft(
    val hour : Int,
    val minute : Int,
    val label : String?,
    val task: String?,
    val taskData: String?,
    val repeatDays: Set<DayOfWeek>, // Use Int to represent DayOfWeek to avoid dependency on java.time in domain layer
    val soundUri: String?,
    val snoozeEnabled: Boolean,
    val snoozeDurationMinutes: Int?,
    val snoozeMaxCount: Int?
)
