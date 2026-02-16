package com.tomcvt.goready.domain

import com.tomcvt.goready.constants.TaskType
import java.time.DayOfWeek

data class AlarmDraft(
    val hour : Int,
    val minute : Int,
    val repeatDays: Set<DayOfWeek>,

    var label : String? = "Alarm",
    var task: TaskType = TaskType.NONE,
    var taskData: String? = null,
    var soundUri: String? = null,
    var snoozeEnabled: Boolean = false,
    var snoozeDurationMinutes: Int? = null,
    var snoozeMaxCount: Int? = null,
    var routineId: Long? = null,
)
