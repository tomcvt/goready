package com.tomcvt.goready.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    val deviceConnection: DeviceConnection by viewModel.deviceConnectionState.collectAsState()

    LaunchedEffect(deviceConnection) {
        Log.d(
            "ScanScreen",
            "savedDevice=${deviceConnection.savedDevice} state=${deviceConnection.connectionState}"
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

    Scaffold { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (deviceConnection.savedDevice != null) {
                DeviceConnectionTile(
                    deviceConnection = deviceConnection,
                    onDisconnect = { viewModel.disconnect() },
                    onForget = { viewModel.forgetDevice() },
                    onClick = { viewModel.connectToSaved() }
                )
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
                            Log.w("ScanScreen", "Missing BLE permissions")
                        }
                        viewModel.saveDeviceAndConnect(deviced.device)
                    })
                }
            }
        }
    }
    /*
    if (showScenarios) {
        BleScenariosModal(viewModel = viewModel, onDismiss = { viewModel.showBleScenariosModal.value = false })
    }
     */
}

@Composable fun DeviceConnectionTile(
    deviceConnection: DeviceConnection,
    onDisconnect: () -> Unit,
    onForget: () -> Unit,
    onClick: () -> Unit
) {
    val device = deviceConnection.savedDevice ?: return
    val connectionState = deviceConnection.connectionState
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
    val deviceName = try { device.name ?: "Unknown" } catch (e: SecurityException) {
        Log.w(TAG, "Missing permission", e)
        "Unknown"
    }
    var menuExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = deviceName, style = MaterialTheme.typography.headlineSmall, color = textColor)
                Text(text = device.address, style = MaterialTheme.typography.bodyMedium, color = textColor)
                Text(text = statusText, style = MaterialTheme.typography.bodyMedium, color = textColor)
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Device options", tint = textColor)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Disconnect") },
                        enabled = connectionState is BleConnectionState.Connected,
                        onClick = { menuExpanded = false; onDisconnect() }
                    )
                    DropdownMenuItem(
                        text = { Text("Forget device") },
                        onClick = { menuExpanded = false; onForget() }
                    )
                }
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

@Composable
fun BleScenariosModal(viewModel: DeviceScanViewModel, onDismiss: () -> Unit) {
    val log by viewModel.scenarioLog.collectAsState()
    val running by viewModel.scenarioRunning.collectAsState()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BackHandler(onBack = onDismiss)
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("BLE Test Scenarios", style = MaterialTheme.typography.headlineSmall)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(12.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    val scenarios = BleScenarios.ALL
                    items(scenarios.size, key = { scenarios[it].id }) { scenarioId ->
                        val scenario = scenarios[scenarioId]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable(enabled = !running) { viewModel.runScenario(scenario.id) },
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(scenario.title, style = MaterialTheme.typography.titleSmall)
                                Text(scenario.description, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                Divider(Modifier.padding(vertical = 8.dp))
                Text("Status", style = MaterialTheme.typography.titleMedium)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 260.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        if (running) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Running…", style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                        if (log.isEmpty() && !running) {
                            Text("No steps run yet.", style = MaterialTheme.typography.bodySmall)
                        }
                        log.forEach { step ->
                            Text(
                                text = "${if (step.success) "✓" else "✗"} ${step.command} → ${step.result}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (step.success) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}