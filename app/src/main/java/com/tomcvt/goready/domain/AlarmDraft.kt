package com.tomcvt.goready.domain

import java.time.DayOfWeek

data class AlarmDraft(
    val hour : Int,
    val minute : Int,
    val repeatDays: Set<DayOfWeek>,

    var label : String? = "Alarm",
    var task: String? = null,
    var taskData: String? = null,
    var soundUri: String? = null,
    var snoozeEnabled: Boolean = false,
    var snoozeDurationMinutes: Int? = null,
    var snoozeMaxCount: Int? = null,
    var routineId: Long? = null,
)
