package com.tomcvt.goready.manager

import com.tomcvt.goready.domain.AlarmDraft
import com.tomcvt.goready.testutil.FakeAlarmRepository
import com.tomcvt.goready.testutil.FakeAlarmScheduler
import com.tomcvt.goready.testutil.FakeTimeProvider
import com.tomcvt.goready.time.RepeatAlarmCalculator
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek

@OptIn(ExperimentalCoroutinesApi::class)
class AppAlarmManagerTest {

    private lateinit var fakeRepo: FakeAlarmRepository
    private lateinit var fakeScheduler: FakeAlarmScheduler
    private lateinit var manager: AppAlarmManager

    @Before
    fun setup() {
        fakeRepo = FakeAlarmRepository()
        fakeScheduler = FakeAlarmScheduler()

        val fakeTime = FakeTimeProvider(
            someFixedMondayAt10amMillis()
        )

        val calculator = RepeatAlarmCalculator(fakeTime)

        manager = AppAlarmManager(
            repository = fakeRepo,
            systemScheduler = fakeScheduler,
            repeatAlarmCalculator = calculator
        )
    }

    private fun someFixedMondayAt10amMillis(): Long {
        return 1736154000 // Monday, 10:00 AM / 6 1 2025
    }

    @Test
    fun `createAlarm with repeat days schedules next alarm`() = runTest {

        val draft = AlarmDraft(
            hour = 12,
            minute = 0,
            label = "Test",
            repeatDays = setOf(DayOfWeek.MONDAY),
            task = null,
            taskData = null,
            soundUri = null,
            snoozeEnabled = false,
            snoozeDurationMinutes = null,
            snoozeMaxCount = null,
            routineId = null
        )

        manager.createAlarm(draft)

        assertNotNull(fakeScheduler.scheduledNext)
        assertNull(fakeScheduler.scheduledOneTime)
    }
}