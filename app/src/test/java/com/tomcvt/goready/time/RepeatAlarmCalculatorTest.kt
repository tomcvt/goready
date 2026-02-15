package com.tomcvt.goready.time

import com.tomcvt.goready.data.AlarmEntity
import com.tomcvt.goready.testutil.FakeTimeProvider
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek

class RepeatAlarmCalculatorTest {

    private fun someFixedMondayAt10amMillis(): Long {
        return 1736154000 // Monday, 10:00 AM / 6 1 2025
    }
    private val mondayAt10Millis = someFixedMondayAt10amMillis()
    private val mondayAt23Millis = mondayAt10Millis + 60 * 60 * 1000 * 13
    private val wednesdayAt9Millis = mondayAt10Millis + 60 * 60 * 1000 * 47



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
            hour = 11,
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