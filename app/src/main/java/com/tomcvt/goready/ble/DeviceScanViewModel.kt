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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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

    fun startScan() { manager.startScan() }

    fun stopScan() { manager.stopScan() }

    fun saveDeviceAndConnect(device: BluetoothDevice) {
        manager.connectAndSave(device)
    }

    override fun onCleared() = manager.stopScan()
}