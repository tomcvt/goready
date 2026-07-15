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

    private fun dateMillis(y: Int, m: Int, d: Int, h: Int, min: Int): Long =
        Calendar.getInstance().apply {
            set(y, m, d, h, min, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    // Tuesday, Jan 7 2025 08:00 / Wednesday, Jan 8 2025 09:00
    private val tuesday8am = dateMillis(2025, Calendar.JANUARY, 7, 8, 0)
    private val wednesday9am = dateMillis(2025, Calendar.JANUARY, 8, 9, 0)

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



    @Test
    fun `today not selected must not fire today even if time has not passed`() {
        val calculator = RepeatAlarmCalculator(FakeTimeProvider(tuesday8am))
        val alarm = AlarmEntity(hour = 9, minute = 0,
            repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
        assertEquals(wednesday9am, calculator.calculateNextAlarmTime(alarm))
        // pre-fix this returns Tuesday 9am
    }
}