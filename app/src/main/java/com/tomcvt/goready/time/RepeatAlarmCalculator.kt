package com.tomcvt.goready.time

import android.util.Log
import com.tomcvt.goready.data.AlarmEntity
import java.time.DayOfWeek
import java.util.Calendar

private const val TAG = "RepeatAlarmCalculator"

class RepeatAlarmCalculator(
    private val timeProvider: TimeProvider
) {
    fun calculateNextAlarmTime(alarm: AlarmEntity): Long {
        val hour = alarm.hour
        val minute = alarm.minute
        val now = timeProvider.now()
        val calendar = Calendar.getInstance()
        if (alarm.repeatDays.isEmpty()) {
            return -1
        }
        if (alarm.repeatDays.size == 7) {
            calendar.apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (calendar.timeInMillis <= now) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
            return calendar.timeInMillis
        }

        if (alarm.repeatDays.size == 1) {
            calendar.apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (calendar.timeInMillis <= now) {
                calendar.add(Calendar.DAY_OF_MONTH, 7)
            }
            return calendar.timeInMillis
        }
        var dayOfWeekInt = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        //TODO FORCE LOCALE OR DO STH BETTER
        val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
        val firstDayOfWeek = calendar.firstDayOfWeek

        val daysInLocaleOrder = mutableListOf<Int>()
        for (i in 0 until 7) {
            val day = (firstDayOfWeek + i - 1) % 7 + 1
            daysInLocaleOrder.add(day)
        }
        val currentIdx = daysInLocaleOrder.indexOf(currentDay)

// 5. Check for the next alarm day
        var daysUntilAlarm = -1

        for (offset in 0..7) {
            val checkIdx = (currentIdx + offset) % 7
            val dayToCheck = daysInLocaleOrder[checkIdx]

            // Convert Calendar day (1..7) to DayOfWeek enum
            // Calendar: SUN=1, MON=2... SAT=7
            // DayOfWeek: MON=1, TUE=2... SUN=7
            val dayOfWeekEnum = when (dayToCheck) {
                Calendar.SUNDAY -> DayOfWeek.SUNDAY
                Calendar.MONDAY -> DayOfWeek.MONDAY
                Calendar.TUESDAY -> DayOfWeek.TUESDAY
                Calendar.WEDNESDAY -> DayOfWeek.WEDNESDAY
                Calendar.THURSDAY -> DayOfWeek.THURSDAY
                Calendar.FRIDAY -> DayOfWeek.FRIDAY
                Calendar.SATURDAY -> DayOfWeek.SATURDAY
                else -> DayOfWeek.MONDAY
            }

            if (alarm.repeatDays.contains(dayOfWeekEnum)) {
                // If it's today, check if the time has already passed
                if (offset == 0) {
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)

                    if (calendar.timeInMillis > now) {
                        daysUntilAlarm = 0
                        break
                    }
                } else {
                    daysUntilAlarm = offset
                    break
                }
            }
        }
        calendar.apply {
            add(Calendar.DAY_OF_MONTH, index - startIndex)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    companion object {
        private val ints = (1..7).toList()
        private val helperDays = ints + ints
        private fun Int.toDayOfWeek(): DayOfWeek {
            return DayOfWeek.of(this)
        }

    }
}