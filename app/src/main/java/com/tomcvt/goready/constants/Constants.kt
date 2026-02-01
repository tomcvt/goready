package com.tomcvt.goready.constants

const val EXTRA_ALARM_ID = "extra_alarm_id"
const val EXTRA_REMAINING_SNOOZE = "extra_remaining_snooze"

const val EXTRA_ROUTINE_SESSION_ID = "extra_routine_session_id"
const val EXTRA_ROUTINE_ID = "extra_routine_id"
const val EXTRA_ROUTINE_INFO = "extra_routine_info"
const val EXTRA_ROUTINE_STEP = "extra_routine_step"

// communication between activity/service and receiver (timeouts too)
const val ACTION_START_ROUTINE = "ACTION_START_ROUTINE"
const val ACTION_STEP_TIMEOUT = "ACTION_STEP_TIMEOUT"
const val ACTION_STEP_COMPLETE = "ACTION_STEP_COMPLETE"
const val ACTION_ROUTINE_COMPLETE = "ACTION_ROUTINE_COMPLETE"


// activity launching intents
const val ACTION_RF_UI_LAUNCHER = "ACTION_RF_UI_LAUNCHER"
const val ACTION_RF_UI_SHOW = "ACTION_RF_UI_SHOW"
const val ACTION_RF_UI_STEP_TIMEOUT = "ACTION_RF_UI_STEP_TIMEOUT"
const val ACTION_RF_UI_STEP_COMPLETE = "ACTION_ROUTINE_STEP_COMPLETE"
//alarms
const val ACTION_ALARM_TRIGGERED = "ACTION_ALARM_TRIGGERED"
const val ACTION_ALARM_SNOOZE_TRIGGERED = "ACTION_ALARM_SNOOZE_TRIGGERED"


enum class TaskType(
    val label: String,
    val code: Long
) {
    NONE("None", 0),
    TIMER("Timer", 1),
    COUNTDOWN("Clicker", 2),
    TEXT("Text", 3),
    MATH("Math tasks", 4),
    TARGET("Target Minigame", 5),
    TASK_CHAIN("Task Chain", 6);



    companion object {
        fun fromLabel(label: String): TaskType? =
            values().find { it.label == label }

        fun fromCode(code: Long): TaskType? =
            values().find { it.code == code }

        fun getList() : List<TaskType> {
            return values().toList()
        }

        fun getTaskTypes() : List<TaskType> {
            return values().filter { it != NONE && it != TASK_CHAIN }
        }
    }
}

enum class MathType(
    val label: String,
    val code: Int
) {
    FIRST("45+65+97=?", 0),
    SECOND("7*65=?", 1),
    THIRD("(13*65)+86=?", 2),
    FOURTH("(13*65)+(17*97)=?", 3);

    companion object {
        fun fromCode(code: Int): MathType? =
            values().find { it.code == code }

        fun getList() : List<MathType> {
            return values().toList()
        }

        fun generateRandomTask(mathType: MathType) : Pair<String, Int> {
            when (mathType) {
                FIRST -> {
                    val first = (10..99).random()
                    val second = (10..99).random()
                    val third = (10..99).random()
                    val question = "$first+$second+$third=?"
                    val answer = first + second + third
                    return Pair(question, answer)
                }
                SECOND -> {
                    val first = (11..20).random()
                    val second = (10..99).random()
                    val question = "$first*$second=?"
                    val answer = first * second
                    return Pair(question, answer)
                }
                THIRD -> {
                    val first = (10..20).random()
                    val second = (10..99).random()
                    val third = (10..99).random()
                    val question = "(${first}*${second})+${third}=?"
                    val answer = (first * second) + third
                    return Pair(question, answer)
                }
                FOURTH -> {
                    val first = (10..20).random()
                    val second = (10..99).random()
                    val third = (10..99).random()
                    val fourth = (10..99).random()
                    val question = "(${first}*${second})+(${third}*${fourth})=?"
                    val answer = (first * second) + (third * fourth)
                    return Pair(question, answer)
                }
            }
        }

        fun generateRandomTaskList(mathType: MathType, count: Int) : List<Pair<String, Int>> {
            val list = mutableListOf<Pair<String, Int>>()
            for (i in 1..count) {
                list.add(generateRandomTask(mathType))
            }
            return list
        }
    }
}

val SNOOZE_MINUTES = listOf(1,3,5,10,15)
val SNOOZE_COUNTS = (1..3).toList()