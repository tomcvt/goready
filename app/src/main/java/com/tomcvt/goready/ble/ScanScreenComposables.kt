package com.tomcvt.goready.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED

private const val TAG = "DeviceScan"

fun hasBlePermissionsC(context: Context): Boolean = if (Build.VERSION.SDK_INT >= 31) {
    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PERMISSION_GRANTED
} else {
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PERMISSION_GRANTED
}

@Composable
fun ScanScreen(viewModel: DeviceScanViewModel, onSelect: (BluetoothDevice) -> Unit) {

    val context = LocalContext.current
    val devices by viewModel.devices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val savedDevice by viewModel.savedDevice.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()


    LaunchedEffect(Unit) { viewModel.startScan() } // auto-scan on nav-in

    Column {
        if (savedDevice != null) {
            ConnectedRow(savedDevice!!, connectionState)
        }
        Button(onClick = { viewModel.startScan() }, enabled = !isScanning) {
            Text(if (isScanning) "Scanning…" else "Scan")
        }
        LazyColumn {
            items(devices.size, key = { devices[it].device.address }) { d ->
                val deviced = devices[d]
                DeviceRow(deviced, onClick = {
                    viewModel.stopScan()
                    if (!hasBlePermissionsC(context)) {
                        Log.w("ScanScreen", "Missing BLE permissions")}
                    viewModel.saveDeviceAndConnect(deviced.device)}
                )
            }
        }
    }
}

@Composable fun ConnectedRow(device: SavedDevice, connectionState: BleConnectionState) {
    val cardColor = when (connectionState) {
        BleConnectionState.Connected -> MaterialTheme.colorScheme.primary
        BleConnectionState.Connecting -> MaterialTheme.colorScheme.secondary
        BleConnectionState.Disconnected -> MaterialTheme.colorScheme.error
    }
    val deviceName = "Unknown"
    try {
        val deviceName = device.name ?: "Unknown"
    } catch (e: SecurityException) {
        Log.w(TAG, "Missing permission", e)
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = cardColor
            ),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = deviceName ?: "Unknown", style = MaterialTheme.typography.headlineSmall)
                Text(text = device.address, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable fun DeviceRow(device: DiscoveredDevice, onClick: () -> Unit) {
    val deviceBt: BluetoothDevice = device.device
    val address = deviceBt.address
    @SuppressLint("MissingPermission")
    val name = "Unknown"
    try {
        val name = deviceBt.name ?: "Unknown"
    } catch (e: SecurityException) {
        Log.w(TAG, "Missing permission", e)
    }
    val rssi = device.rssi
    @SuppressLint("MissingPermission")
    val type = "Unknown"
    try {
        val type = deviceBt.type
    } catch (e: SecurityException) {
        Log.w(TAG, "Missing permission", e)
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = name, style = MaterialTheme.typography.headlineSmall)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = address, style = MaterialTheme.typography.bodyMedium)
                    Text(text = "$rssi dBm", style = MaterialTheme.typography.bodyMedium)
                    Text(text = type, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}