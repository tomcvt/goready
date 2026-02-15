package com.tomcvt.goready.testutil

import com.tomcvt.goready.time.TimeProvider

class FakeTimeProvider(timeMillis: Long) : TimeProvider {
    var currentTime = timeMillis

    override fun now(): Long {
        return currentTime
    }
    fun incrementTime(millis: Long) {
        currentTime += millis
    }
    fun setTime(time: Long) {
        currentTime = time
    }
    fun incrementDays(days: Int) {
        currentTime += days * 24 * 60 * 60 * 1000
    }

    fun addDaysHoursMinutesAndReturn(days: Int, hours: Int, minutes: Int) : Long {
        var millis = days * 24 * 60 * 60 * 1000
        millis += hours * 60 * 60 * 1000
        millis += minutes * 60 * 1000
        return currentTime + millis
    }
}
