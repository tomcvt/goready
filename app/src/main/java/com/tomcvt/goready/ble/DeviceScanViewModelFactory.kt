package com.tomcvt.goready.ble

import android.bluetooth.BluetoothAdapter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tomcvt.goready.viewmodel.AlarmViewModel

class DeviceScanViewModelFactory(
    private val adapter: BluetoothAdapter,
    private val manager: BleDeviceManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeviceScanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DeviceScanViewModel(adapter, manager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}