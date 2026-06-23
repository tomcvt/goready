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

    private val _devices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val devices: StateFlow<List<DiscoveredDevice>> = _devices

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val seen = mutableMapOf<String, DiscoveredDevice>()

    val savedDevice: StateFlow<SavedDevice?> = manager.savedDevice
    val connectionState: StateFlow<BleConnectionState> = manager.connectionState

    private val callback = object : ScanCallback() {
        override fun onScanResult(type: Int, result: ScanResult) {
            val mfg = result.scanRecord?.getManufacturerSpecificData(0xFFFF)
            val deviceType = mfg?.getOrNull(2)?.toInt() // byte after "AL"
            try {
                seen[result.device.address] =
                    DiscoveredDevice(result.device, result.device.name, result.rssi, deviceType)
            } catch (e: SecurityException) {
                //TODO resolve this correctly
                Log.w(TAG, "Security exception: $e")
                return
            }
            _devices.value = seen.values.sortedByDescending { it.rssi }
        }
        override fun onScanFailed(errorCode: Int) { _isScanning.value = false }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (_isScanning.value) return
        //check permission, log debug when not granted
        /*
        if (hasBlePermissions(context).not()) {
            Log.w(TAG, "Missing BLE permissions")
            return
        }*/
        seen.clear(); _devices.value = emptyList(); _isScanning.value = true

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
            .build()
        scanner.startScan(
            listOf(filter),
            ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),
            callback
        )
        viewModelScope.launch { delay(8000); stopScan() } // don't let scans run forever
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!_isScanning.value) return
        scanner.stopScan(callback)
        _isScanning.value = false
    }

    fun saveDeviceAndConnect(device: BluetoothDevice) {
        manager.connectAndSave(device)
    }

    override fun onCleared() = stopScan()
}