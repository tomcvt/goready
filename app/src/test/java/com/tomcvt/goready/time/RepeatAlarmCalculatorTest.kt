package com.tomcvt.goready.time

import com.tomcvt.goready.data.AlarmEntity
import com.tomcvt.goready.testutil.FakeTimeProvider
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.util.Calendar

class RepeatAlarmCalculatorTest {

    private fun someFixedMondayAt10amMillis(): Long {
        return Calendar.getInstance().apply {
            set(2025, Calendar.JANUARY, 6, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis // Monday, 10:00 AM / 6 1 2025
    }
    private val mondayAt10Millis = someFixedMondayAt10amMillis()
    private val mondayAt23Millis = mondayAt10Millis + 60 * 60 * 1000 * 13
    private val wednesdayAt9Millis = Calendar.getInstance().apply{
        timeInMillis = mondayAt10Millis
        set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY)
        set(Calendar.HOUR_OF_DAY, 9)
        set(Calendar.MINUTE, 0)
    }.timeInMillis




    @Test
    fun `test calculations`() {
        val fakeTime = FakeTimeProvider(
            mondayAt10Millis
        )

        val calculator = RepeatAlarmCalculator(fakeTime)

        val alarm = AlarmEntity(
            hour = 23,
            minute = 0,
            repeatDays = setOf(DayOfWeek.MONDAY)
        )
        val alarm2 = AlarmEntity(
            hour = 9,
            minute = 0,
            repeatDays = setOf(DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,DayOfWeek.MONDAY)
        )
        val alarm3 = AlarmEntity(
            hour = 12,
            minute = 0,
            repeatDays = emptySet()
        )

        val result1 = calculator.calculateNextAlarmTime(alarm)
        val result2 = calculator.calculateNextAlarmTime(alarm2)
        val result3 = calculator.calculateNextAlarmTime(alarm3)

        assertEquals(mondayAt23Millis, result1)
        assertEquals(wednesdayAt9Millis, result2)
        assertEquals(-1, result3)
    }
}