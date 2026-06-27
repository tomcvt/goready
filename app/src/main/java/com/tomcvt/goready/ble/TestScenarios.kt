package com.tomcvt.goready.ble

data class BleScenario(
    val id: String,
    val title: String,
    val description: String
)

object BleScenarios {
    val ALL = listOf(
        BleScenario(
            id = "clock_sync_quick_alarm",
            title = "Clock sync → quick alarm → snooze → stop",
            description = "Syncs the device clock 5s ahead, plays alarm #4, snoozes it, then stops it."
        )
        // add more scenarios here, with a matching branch in DeviceScanViewModel.runScenario()
    )
}

data class ScenarioStepResult(
    val command: String,
    val result: String,
    val success: Boolean
)