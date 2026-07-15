package com.tomcvt.goready.manager

import com.tomcvt.goready.constants.TaskType
import com.tomcvt.goready.data.AlarmEntity
import com.tomcvt.goready.domain.AlarmDraft
import com.tomcvt.goready.testutil.FakeAlarmRepository2
import com.tomcvt.goready.testutil.FakeAlarmScheduler2
import com.tomcvt.goready.testutil.FakeTimeProvider
import com.tomcvt.goready.time.RepeatAlarmCalculator
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.DayOfWeek
import java.util.Calendar

class AppAlarmManagerImplTest {

    private fun dateMillis(y: Int, m: Int, d: Int, h: Int, min: Int): Long =
        Calendar.getInstance().apply {
            set(y, m, d, h, min, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    // Tuesday, Jan 7 2025 08:00 / Wednesday, Jan 8 2025 09:00
    private val tuesday8am = dateMillis(2025, Calendar.JANUARY, 7, 8, 0)
    private val wednesday9am = dateMillis(2025, Calendar.JANUARY, 8, 9, 0)
    private val tuesday9am = dateMillis(2025, Calendar.JANUARY, 7, 9, 0)
    private val scheduler = FakeAlarmScheduler2()
    private val repository = FakeAlarmRepository2() // in-memory, backed by a MutableStateFlow<List<AlarmEntity>>
    private val calculator = RepeatAlarmCalculator(FakeTimeProvider(tuesday8am))
    private val manager = AppAlarmManagerImpl(repository, scheduler, calculator)

    /*
    @Test
    fun `trigger with no repeat days disables alarm and does not reschedule`() = runTest {
        val id = repository.insertAlarm(
            AlarmEntity(
                hour = 9,
                minute = 0,
                repeatDays = emptySet(),
                isEnabled = true
            )
        )
        manager.scheduleNextAlarmOrDisable(id)
        assertFalse(repository.getAlarmById(id)!!.isEnabled)
        assertTrue(scheduler.scheduledNext.isEmpty())
    }

    @Test
    fun `trigger with repeat days reschedules for correct next day and stays enabled`() = runTest {
        val id = repository.insertAlarm(AlarmEntity(hour = 9, minute = 0,
            repeatDays = setOf(DayOfWeek.WEDNESDAY), isEnabled = true))
        manager.scheduleNextAlarmOrDisable(id)
        assertTrue(repository.getAlarmById(id)!!.isEnabled)
        assertEquals(id to wednesday9am, scheduler.scheduledNext.single())
    }


     */
    @Test
    fun `toggling off cancels regardless of repeat days`() = runTest {
        val alarm = AlarmEntity(id = 1, hour = 9, minute = 0,
            repeatDays = setOf(DayOfWeek.MONDAY), isEnabled = true)
        manager.toggleAlarm(alarm, false)
        assertEquals(listOf(1L), scheduler.cancelled)
    }

    @Test
    fun `update from recurring to no-days switches to one-time scheduling`() = runTest {
        val id = repository.insertAlarm(AlarmEntity(hour = 9, minute = 0,
            repeatDays = setOf(DayOfWeek.MONDAY), isEnabled = true))
        manager.updateAlarm(
            AlarmDraft(
                hour = 9,
                minute = 0,
                repeatDays = emptySet(),
                task = TaskType.NONE
            ), id)
        assertTrue(scheduler.scheduledOneTime.contains(id))
        assertTrue(scheduler.scheduledNext.none { it.first == id })
    }
    // Repeat-day fixtures used across the matrix.
    // "partial" deliberately excludes Tuesday so it shares the same expected
    // next-occurrence (Wednesday 9am) as your original single-day test.
    private val noDays = emptySet<DayOfWeek>()
    private val partialDays = setOf(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
    private val allDays = DayOfWeek.values().toSet()

    // ---------------------------------------------------------------------
    // TRIGGER (already covered in your original file — kept here for context,
    // not duplicated)
    // ---------------------------------------------------------------------

    @Test
    fun `trigger with no repeat days disables alarm and does not reschedule`() = runTest {
        val id = repository.insertAlarm(
            AlarmEntity(hour = 9, minute = 0, repeatDays = emptySet(), isEnabled = true)
        )
        manager.scheduleNextAlarmOrDisable(id)
        assertFalse(repository.getAlarmById(id)!!.isEnabled)
        assertTrue(scheduler.scheduledNext.isEmpty())
    }

    @Test
    fun `trigger with repeat days reschedules for correct next day and stays enabled`() = runTest {
        val id = repository.insertAlarm(
            AlarmEntity(hour = 9, minute = 0, repeatDays = setOf(DayOfWeek.WEDNESDAY), isEnabled = true)
        )
        manager.scheduleNextAlarmOrDisable(id)
        assertTrue(repository.getAlarmById(id)!!.isEnabled)
        assertEquals(id to wednesday9am, scheduler.scheduledNext.single())
    }

    // ---------------------------------------------------------------------
    // CREATE
    // isEnabled is hardcoded to true in AppAlarmManagerImpl.createAlarm(),
    // regardless of what's in the draft — there is no "create x disabled" cell
    // to test, since the implementation never produces a disabled alarm on
    // creation. These three rows cover the {empty, partial, all-7} axis only.
    // ---------------------------------------------------------------------

    @Test
    fun `create - empty days - is always enabled and schedules one-time`() = runTest {
        manager.createAlarm(
            AlarmDraft(hour = 9, minute = 0, repeatDays = noDays, task = TaskType.NONE)
        )
        val alarm = repository.getAlarms().value.single()
        assertTrue(alarm.isEnabled)
        assertTrue(scheduler.scheduledOneTime.contains(alarm.id))
        assertTrue(scheduler.scheduledNext.none { it.first == alarm.id })
    }

    @Test
    fun `create - partial days - is always enabled and schedules next occurrence`() = runTest {
        manager.createAlarm(
            AlarmDraft(hour = 9, minute = 0, repeatDays = partialDays, task = TaskType.NONE)
        )
        val alarm = repository.getAlarms().value.single()
        assertTrue(alarm.isEnabled)
        assertEquals(alarm.id to wednesday9am, scheduler.scheduledNext.single())
        assertFalse(scheduler.scheduledOneTime.contains(alarm.id))
    }

    @Test
    fun `create - all-7 days - is always enabled and schedules next occurrence`() = runTest {
        manager.createAlarm(
            AlarmDraft(hour = 9, minute = 0, repeatDays = allDays, task = TaskType.NONE)
        )
        val alarm = repository.getAlarms().value.single()
        assertTrue(alarm.isEnabled)
        assertEquals(alarm.id to tuesday9am, scheduler.scheduledNext.single())
        assertFalse(scheduler.scheduledOneTime.contains(alarm.id))
    }

    // ---------------------------------------------------------------------
    // UPDATE
    // updateAlarm() never overwrites isEnabled from the draft — it's carried
    // over from the existing entity. So "enabled/disabled" here means
    // "was the alarm already enabled before this update," and scheduling
    // should follow from that pre-existing state + the new repeatDays.
    // ---------------------------------------------------------------------

    @Test
    fun `update - empty days - was enabled - switches to one-time scheduling`() = runTest {
        val id = repository.insertAlarm(
            AlarmEntity(hour = 9, minute = 0, repeatDays = setOf(DayOfWeek.MONDAY), isEnabled = true)
        )
        manager.updateAlarm(AlarmDraft(hour = 9, minute = 0, repeatDays = noDays, task = TaskType.NONE), id)
        assertTrue(repository.getAlarmById(id)!!.isEnabled)
        assertTrue(scheduler.scheduledOneTime.contains(id))
        assertTrue(scheduler.scheduledNext.none { it.first == id })
        assertEquals(listOf(id), scheduler.cancelled) // old alarm cancelled before rescheduling
    }

    @Test
    fun `update - partial days - was enabled - schedules next occurrence`() = runTest {
        val id = repository.insertAlarm(
            AlarmEntity(hour = 9, minute = 0, repeatDays = noDays, isEnabled = true)
        )
        manager.updateAlarm(AlarmDraft(hour = 9, minute = 0, repeatDays = partialDays, task = TaskType.NONE), id)
        assertTrue(repository.getAlarmById(id)!!.isEnabled)
        assertEquals(id to wednesday9am, scheduler.scheduledNext.single())
        assertFalse(scheduler.scheduledOneTime.contains(id))
    }

    @Test
    fun `update - all-7 days - was enabled - schedules next occurrence`() = runTest {
        val id = repository.insertAlarm(
            AlarmEntity(hour = 9, minute = 0, repeatDays = noDays, isEnabled = true)
        )
        manager.updateAlarm(AlarmDraft(hour = 9, minute = 0, repeatDays = allDays, task = TaskType.NONE), id)
        assertTrue(repository.getAlarmById(id)!!.isEnabled)
        assertEquals(id to tuesday9am, scheduler.scheduledNext.single())
        assertFalse(scheduler.scheduledOneTime.contains(id))
    }

    @Test
    fun `update - empty days - was disabled - stays cancelled, nothing scheduled`() = runTest {
        val id = repository.insertAlarm(
            AlarmEntity(hour = 9, minute = 0, repeatDays = noDays, isEnabled = false)
        )
        manager.updateAlarm(AlarmDraft(hour = 9, minute = 0, repeatDays = noDays, task = TaskType.NONE), id)
        assertFalse(repository.getAlarmById(id)!!.isEnabled)
        assertTrue(scheduler.scheduledOneTime.isEmpty())
        assertTrue(scheduler.scheduledNext.isEmpty())
        assertTrue(scheduler.cancelled.contains(id))
    }

    @Test
    fun `update - partial days - was disabled - stays cancelled, nothing scheduled`() = runTest {
        val id = repository.insertAlarm(
            AlarmEntity(hour = 9, minute = 0, repeatDays = setOf(DayOfWeek.MONDAY), isEnabled = false)
        )
        manager.updateAlarm(AlarmDraft(hour = 9, minute = 0, repeatDays = partialDays, task = TaskType.NONE), id)
        assertFalse(repository.getAlarmById(id)!!.isEnabled)
        assertTrue(scheduler.scheduledOneTime.isEmpty())
        assertTrue(scheduler.scheduledNext.isEmpty())
        assertTrue(scheduler.cancelled.contains(id))
    }

    @Test
    fun `update - all-7 days - was disabled - stays cancelled, nothing scheduled`() = runTest {
        val id = repository.insertAlarm(
            AlarmEntity(hour = 9, minute = 0, repeatDays = setOf(DayOfWeek.MONDAY), isEnabled = false)
        )
        manager.updateAlarm(AlarmDraft(hour = 9, minute = 0, repeatDays = allDays, task = TaskType.NONE), id)
        assertFalse(repository.getAlarmById(id)!!.isEnabled)
        assertTrue(scheduler.scheduledOneTime.isEmpty())
        assertTrue(scheduler.scheduledNext.isEmpty())
        assertTrue(scheduler.cancelled.contains(id))
    }

    // ---------------------------------------------------------------------
    // TOGGLE
    // The empty-days + enable case is the one explicitly called out in the
    // spec: toggling on an alarm with no recurring days should schedule it
    // once for the next occurrence of its hour/minute, not leave it inert.
    // ---------------------------------------------------------------------

    @Test
    fun `toggle - empty days - enabling schedules one-time for next date`() = runTest {
        val alarm = AlarmEntity(id = 1, hour = 9, minute = 0, repeatDays = noDays, isEnabled = false)
        manager.toggleAlarm(alarm, true)
        assertTrue(scheduler.scheduledOneTime.contains(1L))
        assertTrue(scheduler.scheduledNext.none { it.first == 1L })
        assertTrue(scheduler.cancelled.isEmpty())
    }

    @Test
    fun `toggle - empty days - disabling cancels`() = runTest {
        val alarm = AlarmEntity(id = 2, hour = 9, minute = 0, repeatDays = noDays, isEnabled = true)
        manager.toggleAlarm(alarm, false)
        assertEquals(listOf(2L), scheduler.cancelled)
        assertFalse(scheduler.scheduledOneTime.contains(2L))
        assertTrue(scheduler.scheduledNext.none { it.first == 2L })
    }

    @Test
    fun `toggle - partial days - enabling schedules next occurrence`() = runTest {
        val alarm = AlarmEntity(id = 3, hour = 9, minute = 0, repeatDays = partialDays, isEnabled = false)
        manager.toggleAlarm(alarm, true)
        assertEquals(3L to wednesday9am, scheduler.scheduledNext.single())
        assertFalse(scheduler.scheduledOneTime.contains(3L))
    }

    @Test
    fun `toggle - partial days - disabling cancels regardless of repeat days`() = runTest {
        val alarm = AlarmEntity(id = 4, hour = 9, minute = 0, repeatDays = partialDays, isEnabled = true)
        manager.toggleAlarm(alarm, false)
        assertEquals(listOf(4L), scheduler.cancelled)
    }

    @Test
    fun `toggle - all-7 days - enabling schedules next occurrence`() = runTest {
        val alarm = AlarmEntity(id = 5, hour = 9, minute = 0, repeatDays = allDays, isEnabled = false)
        manager.toggleAlarm(alarm, true)
        assertEquals(5L to tuesday9am, scheduler.scheduledNext.single())
        assertFalse(scheduler.scheduledOneTime.contains(5L))
    }

    @Test
    fun `toggle - all-7 days - disabling cancels`() = runTest {
        val alarm = AlarmEntity(id = 6, hour = 9, minute = 0, repeatDays = allDays, isEnabled = true)
        manager.toggleAlarm(alarm, false)
        assertEquals(listOf(6L), scheduler.cancelled)
    }

    // ---------------------------------------------------------------------
    // DELETE
    // deleteAlarm() doesn't branch on days or enabled state — it always
    // removes from the repo and cancels the underlying system alarm. These
    // six tests exist to lock that in as a regression guard, since it would
    // be easy to accidentally special-case delete the way update/toggle are.
    // ---------------------------------------------------------------------

    @Test
    fun `delete - empty days - enabled - cancels and removes`() = runTest {
        val alarm = AlarmEntity(id = 7, hour = 9, minute = 0, repeatDays = noDays, isEnabled = true)
        repository.insertAlarm(alarm)
        manager.deleteAlarm(alarm)
        assertEquals(listOf(7L), scheduler.cancelled)
        assertNull(repository.getAlarmById(7L))
    }

    @Test
    fun `delete - empty days - disabled - cancels and removes`() = runTest {
        val alarm = AlarmEntity(id = 8, hour = 9, minute = 0, repeatDays = noDays, isEnabled = false)
        repository.insertAlarm(alarm)
        manager.deleteAlarm(alarm)
        assertEquals(listOf(8L), scheduler.cancelled)
        assertNull(repository.getAlarmById(8L))
    }

    @Test
    fun `delete - partial days - enabled - cancels and removes`() = runTest {
        val alarm = AlarmEntity(id = 9, hour = 9, minute = 0, repeatDays = partialDays, isEnabled = true)
        repository.insertAlarm(alarm)
        manager.deleteAlarm(alarm)
        assertEquals(listOf(9L), scheduler.cancelled)
        assertNull(repository.getAlarmById(9L))
    }

    @Test
    fun `delete - partial days - disabled - cancels and removes`() = runTest {
        val alarm = AlarmEntity(id = 10, hour = 9, minute = 0, repeatDays = partialDays, isEnabled = false)
        repository.insertAlarm(alarm)
        manager.deleteAlarm(alarm)
        assertEquals(listOf(10L), scheduler.cancelled)
        assertNull(repository.getAlarmById(10L))
    }

    @Test
    fun `delete - all-7 days - enabled - cancels and removes`() = runTest {
        val alarm = AlarmEntity(id = 11, hour = 9, minute = 0, repeatDays = allDays, isEnabled = true)
        repository.insertAlarm(alarm)
        manager.deleteAlarm(alarm)
        assertEquals(listOf(11L), scheduler.cancelled)
        assertNull(repository.getAlarmById(11L))
    }

    @Test
    fun `delete - all-7 days - disabled - cancels and removes`() = runTest {
        val alarm = AlarmEntity(id = 12, hour = 9, minute = 0, repeatDays = allDays, isEnabled = false)
        repository.insertAlarm(alarm)
        manager.deleteAlarm(alarm)
        assertEquals(listOf(12L), scheduler.cancelled)
        assertNull(repository.getAlarmById(12L))
    }
}