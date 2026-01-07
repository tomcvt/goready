package com.tomcvt.goready.preview

import com.tomcvt.goready.data.AlarmEntity

class PreviewAlarms2() {
    val alarm1 = AlarmEntity(
        hour = 7,
        minute = 30,
        isEnabled = true,
        label = "Morning Alarm",
        task = "math_quiz",
        taskData = "easy",
        repeatDays = setOf(),
        soundUri = null,
        snoozeEnabled = true,
        snoozeDurationMinutes = 10,
        snoozeMaxCount = 3
    )
    val alarm2 = AlarmEntity(
        hour = 22,
        minute = 0,
        isEnabled = false,
        label = "Bedtime Reminder",
        task = "none",
        taskData = null,
        repeatDays = setOf(),
        soundUri = null,
        snoozeEnabled = false,
        snoozeDurationMinutes = null,
        snoozeMaxCount = null
    )
    val alarmList = listOf(alarm1, alarm2)
}
