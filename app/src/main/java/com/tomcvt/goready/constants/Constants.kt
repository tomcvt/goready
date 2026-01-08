package com.tomcvt.goready.constants

const val EXTRA_ALARM_ID = "extra_alarm_id"

enum class TaskType(
    val label: String,
    val code: Long
) {
    NONE("None", 0),
    TIMER("Timer", 1),
    COUNTDOWN("Countdown", 2),
    TEXT("Text", 3),
    MATH("Email", 4);

    companion object {
        fun fromLabel(label: String): TaskType? =
            values().find { it.label == label }

        fun fromCode(code: Long): TaskType? =
            values().find { it.code == code }
    }

}