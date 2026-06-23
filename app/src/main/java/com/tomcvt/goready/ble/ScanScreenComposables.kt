package com.tomcvt.goready.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

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
    val savedDevice: SavedDevice? by viewModel.savedDevice.collectAsState()
    val connectionState: BleConnectionState by viewModel.connectionState.collectAsState()

    LaunchedEffect(savedDevice, connectionState) {
        Log.d(
            "ScanScreen",
            "savedDevice=$savedDevice state=$connectionState"
        )
    }

    val ble = rememberBlePermissionController()
    var dismissedLocationDialog by rememberSaveable { mutableStateOf(false) }

    // forget the dismissal from backgrounding
    LaunchedEffect(ble.state.locationServicesOn) {
        if (ble.state.locationServicesOn) dismissedLocationDialog = false
    }

    LaunchedEffect(ble.state.isReady) {
        if (ble.state.isReady) viewModel.startScan()
    }

    if (!ble.state.locationServicesOn && !dismissedLocationDialog) {
        LocationDisabledDialog(
            onConfirm = { ble.openLocationSettings() },
            onDismiss = { dismissedLocationDialog = true }
        )
    }

    Column {
        if (!ble.state.isReady) {
            if (!ble.state.canScan || !ble.state.canConnect) {
                Button(onClick = ble.requestPermissions) { Text("Grant Bluetooth permissions") }
            } else if (!ble.state.locationServicesOn) {
                Button(onClick = ble.openLocationSettings) { Text("Turn on Location") }
            }
            return@Column
        }
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
    val textColor = when (connectionState) {
        BleConnectionState.Connected -> MaterialTheme.colorScheme.onPrimary
        BleConnectionState.Connecting -> MaterialTheme.colorScheme.onSecondary
        BleConnectionState.Disconnected -> MaterialTheme.colorScheme.onError
    }
    val statusText = when (connectionState) {
        BleConnectionState.Connected -> "Connected"
        BleConnectionState.Connecting -> "Connecting…"
        BleConnectionState.Disconnected -> "Disconnected"
    }
    var deviceName = "Unknown"
    try {
        deviceName = device.name ?: "Unknown"
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
                Text(text = deviceName ?: "Unknown", style = MaterialTheme.typography.headlineSmall, color = textColor)
                Text(text = device.address, style = MaterialTheme.typography.bodyMedium, color = textColor)
                Text(text = statusText, style = MaterialTheme.typography.bodyMedium, color = textColor)
            }
        }
    }
}

@Composable fun DeviceRow(device: DiscoveredDevice, onClick: () -> Unit) {
    val deviceBt: BluetoothDevice = device.device
    val address = deviceBt.address
    @SuppressLint("MissingPermission")
    var name = "Unknown"
    try {
        name = deviceBt.name ?: "Unknown"
    } catch (e: SecurityException) {
        Log.w(TAG, "Missing permission", e)
    }
    var rssi = device.rssi
    @SuppressLint("MissingPermission")
    var type = 0
    try {
        type = deviceBt.type
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
                    Text(text = type.toString(), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

class BlePermissionController(
    val state: BlePermissionState,
    val requestPermissions: () -> Unit,
    val openLocationSettings: () -> Unit
)

@Composable
fun rememberBlePermissionController(): BlePermissionController {
    val context = LocalContext.current
    val manager = remember { BlePermissionManager(context.applicationContext) }
    val state by manager.state.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { manager.refresh() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) manager.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return BlePermissionController(
        state = state,
        requestPermissions = { launcher.launch(manager.permissionsToRequest()) },
        openLocationSettings = { manager.openLocationSettings() }
    )
}

@Composable
fun LocationDisabledDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Location services required") },
        text = {
            Text(
                "On this Android version, Bluetooth scanning requires Location services " +
                        "to be turned on system-wide — this is an OS restriction, not something " +
                        "the app uses your location for. Turn it on to scan for devices."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Open Settings") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Not now") }
        }
    )
}