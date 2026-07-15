package com.tomcvt.goready.time

import android.util.Log
import com.tomcvt.goready.data.AlarmEntity
import java.time.DateTimeException
import java.time.DayOfWeek
import java.util.Calendar

private const val TAG = "RepeatAlarmCalculator"
private const val A_DAY_IN_MILLIS = 24 * 60 * 60 * 1000

class RepeatAlarmCalculator(
    private val timeProvider: TimeProvider
) {
    fun calculateNextAlarmTime(alarm: AlarmEntity): Long {
        val hour = alarm.hour
        val minute = alarm.minute
        val now = timeProvider.now()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = now
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
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        if (calendar.timeInMillis >= now) {
            return calendar.timeInMillis
        }
        val currentDay = calendarToIso(calendar.get(Calendar.DAY_OF_WEEK))
        val startIndex = helperDays.indexOf(currentDay)

        var dayIndex = startIndex + 1
        for (i in 0..7) {
            if (alarm.repeatDays.contains(DayOfWeek.of(helperDays[dayIndex]))) {
                break
            }
            dayIndex = (dayIndex + 1) % helperDays.size
        }
        calendar.add(Calendar.DAY_OF_MONTH, dayIndex - startIndex)
        return calendar.timeInMillis
    }

    fun calendarToIso(calendarDay: Int): Int {
        return if (calendarDay == Calendar.SUNDAY) {
            7
        } else {
            calendarDay - 1
        }
    }

    fun dayofWeek(day: Int): DayOfWeek {
        if (day < 1 || day > 7) {
            throw DateTimeException("Invalid value for DayOfWeek: " + day);
        }
        when (day) {
            1 -> return DayOfWeek.MONDAY
            2 -> return DayOfWeek.TUESDAY
            3 -> return DayOfWeek.WEDNESDAY
            4 -> return DayOfWeek.THURSDAY
            5 -> return DayOfWeek.FRIDAY
            6 -> return DayOfWeek.SATURDAY
            7 -> return DayOfWeek.SUNDAY
        }
        return DayOfWeek.MONDAY
    }

    companion object {
        private val ints = (1..7).toList()
        private val helperDays = ints + ints
    }
}