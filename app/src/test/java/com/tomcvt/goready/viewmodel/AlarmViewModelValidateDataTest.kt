package com.tomcvt.goready.viewmodel

import com.tomcvt.goready.constants.TaskType
import com.tomcvt.goready.testutil.MockAppAlarmManager
import com.tomcvt.goready.testutil.MockAppRoutinesManager
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class ValidateDataTest {

    private lateinit var viewModel: AlarmViewModel // Replace with actual ViewModel

    @Before
    fun setup() {
        viewModel = AlarmViewModel(
            appAlarmManager = MockAppAlarmManager(),
            routinesManager = MockAppRoutinesManager()
        )
    }

    // -------------------------
    // TaskType.NONE
    // -------------------------

    @Test
    fun `NONE returns true for blank input`() {
        assertTrue(viewModel.validateData(TaskType.NONE, ""))
    }

    @Test
    fun `NONE returns true for random input`() {
        assertTrue(viewModel.validateData(TaskType.NONE, "anything"))
    }

    // -------------------------
    // TaskType.TIMER
    // -------------------------

    @Test
    fun `TIMER returns true for blank input`() {
        assertTrue(viewModel.validateData(TaskType.TIMER, ""))
    }

    @Test
    fun `TIMER returns true for numeric input`() {
        assertTrue(viewModel.validateData(TaskType.TIMER, "123"))
    }

    // -------------------------
    // TaskType.COUNTDOWN
    // -------------------------

    @Test
    fun `COUNTDOWN returns false when blank`() {
        assertFalse(viewModel.validateData(TaskType.COUNTDOWN, ""))
    }

    @Test
    fun `COUNTDOWN returns false when contains letters`() {
        assertFalse(viewModel.validateData(TaskType.COUNTDOWN, "10a"))
    }

    @Test
    fun `COUNTDOWN returns false when zero`() {
        assertFalse(viewModel.validateData(TaskType.COUNTDOWN, "0"))
    }

    @Test
    fun `COUNTDOWN returns false when negative`() {
        assertFalse(viewModel.validateData(TaskType.COUNTDOWN, "-5"))
    }

    @Test
    fun `COUNTDOWN returns true when positive number`() {
        assertTrue(viewModel.validateData(TaskType.COUNTDOWN, "10"))
    }

    @Test
    fun `COUNTDOWN passes when number exceeds Int range`() {
        assertFalse(viewModel.validateData(TaskType.COUNTDOWN, "999999999999999999"))
    }

    // -------------------------
    // TaskType.TEXT
    // -------------------------

    @Test
    fun `TEXT returns false when blank`() {
        assertFalse(viewModel.validateData(TaskType.TEXT, ""))
    }

    @Test
    fun `TEXT returns false when spaces only`() {
        assertFalse(viewModel.validateData(TaskType.TEXT, "   "))
    }

    @Test
    fun `TEXT returns true when not blank`() {
        assertTrue(viewModel.validateData(TaskType.TEXT, "Hello"))
    }

    // -------------------------
    // TaskType.MATH
    // -------------------------

    @Test
    fun `MATH returns false when no delimiter`() {
        assertFalse(viewModel.validateData(TaskType.MATH, "ADD5"))
    }

    @Test
    fun `MATH returns false when too many parts`() {
        assertFalse(viewModel.validateData(TaskType.MATH, "ADD|5|extra"))
    }

    @Test
    fun `MATH returns false when value not numeric`() {
        assertFalse(viewModel.validateData(TaskType.MATH, "ADD|abc"))
    }

    @Test
    fun `MATH returns false when enum invalid`() {
        assertFalse(viewModel.validateData(TaskType.MATH, "INVALID|5"))
    }

    @Test
    fun `MATH returns true when valid enum and number`() {
        // Replace ADD with an actual valid MathType value in your project
        assertTrue(viewModel.validateData(TaskType.MATH, "FIRST|5"))
    }

    @Test
    fun `MATH doesnt allow zero value`() {
        assertFalse(viewModel.validateData(TaskType.MATH, "FIRST|0"))
    }

    // -------------------------
    // TaskType.TARGET
    // -------------------------

    @Test
    fun `TARGET returns false when blank`() {
        assertFalse(viewModel.validateData(TaskType.TARGET, ""))
    }

    @Test
    fun `TARGET returns false when zero`() {
        assertFalse(viewModel.validateData(TaskType.TARGET, "0"))
    }

    @Test
    fun `TARGET returns true when positive`() {
        assertTrue(viewModel.validateData(TaskType.TARGET, "15"))
    }

    @Test
    fun `TARGET passes when number exceeds Int range`() {
        val result = viewModel.validateData(TaskType.TARGET, "999999999999999999")
        assertFalse(result)
    }
}