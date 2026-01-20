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

        fun getList() : List<TaskType> {
            return values().toList()
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