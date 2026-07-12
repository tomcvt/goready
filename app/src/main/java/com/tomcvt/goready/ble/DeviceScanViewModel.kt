package com.tomcvt.goready.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.TimeZone

private const val TAG = "DeviceScanViewModel"

class DeviceScanViewModel(
    private val adapter: BluetoothAdapter,
    private val manager: BleDeviceManager
) : ViewModel() {
    private val scanner get() = adapter.bluetoothLeScanner

    val devices: StateFlow<List<DiscoveredDevice>> = manager.scanResults

    val isScanning: StateFlow<Boolean> = manager.isScanning

    private val seen = mutableMapOf<String, DiscoveredDevice>()

    val savedDevice: StateFlow<SavedDevice?> = manager.savedDevice
    val connectionState: StateFlow<BleConnectionState> = manager.connectionState

    val deviceConnectionState: StateFlow<DeviceConnection> = manager.deviceConnectionState

    val events: SharedFlow<BleEvent> = manager.events

    //-------
    val showBleScenariosModal = MutableStateFlow(false)

    private val _scenarioLog = MutableStateFlow<List<ScenarioStepResult>>(emptyList())
    val scenarioLog: StateFlow<List<ScenarioStepResult>> = _scenarioLog

    private val _scenarioRunning = MutableStateFlow(false)
    val scenarioRunning: StateFlow<Boolean> = _scenarioRunning

    //----------
    fun startScan() { manager.startScan() }

    fun stopScan() { manager.stopScan() }

    fun saveDeviceAndConnect(device: BluetoothDevice) {
        manager.connectAndSave(device)
    }

    fun disconnect() { manager.disconnect() }

    fun forgetDevice() { manager.forgetDevice() }

    fun connectToSaved() { manager.connectToSaved() }

    override fun onCleared() = manager.stopScan()

    //TEST scenarios

    fun runScenario(scenarioId: String) {
        if (_scenarioRunning.value) return
        viewModelScope.launch {
            _scenarioRunning.value = true
            _scenarioLog.value = emptyList()
            try {
                when (scenarioId) {
                    "clock_sync_quick_alarm" -> runClockSyncQuickAlarmScenario()
                    else -> Log.w(TAG, "Unknown scenario: $scenarioId")
                }
            } finally {
                _scenarioRunning.value = false
            }
        }
    }

    private suspend fun runClockSyncQuickAlarmScenario() {
        val offset = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000
        val nowPlus5 = System.currentTimeMillis() / 1000 + 5

        runStep("TIME:$nowPlus5:$offset")
        delay(5000)

        runStep(manager.encodeAlarmPlayCommand(4))
        delay(10000)

        runStep(manager.encodeAlarmSnoozeCommand(4, 5))
        delay(10000)

        runStep(manager.encodeAlarmStopCommand(4))
    }

    private suspend fun runStep(command: String) {
        val result = manager.request(command)
        val resultText = result.fold(
            onSuccess = { it },
            onFailure = { e -> if (e is TimeoutCancellationException) "timed out" else (e.message ?: "error") }
        )
        _scenarioLog.value = _scenarioLog.value + ScenarioStepResult(command, resultText, result.isSuccess)
    }
}