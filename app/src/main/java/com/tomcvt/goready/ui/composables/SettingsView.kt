package com.tomcvt.goready.ui.composables

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tomcvt.goready.BuildConfig
import com.tomcvt.goready.MainActivity
import com.tomcvt.goready.ble.BleScenariosModal
import com.tomcvt.goready.ble.DeviceScanViewModel
import com.tomcvt.goready.ble.ScanScreen
import com.tomcvt.goready.domain.PermissionSpec
import com.tomcvt.goready.registries.getPermissionRegistryForSdk
import com.tomcvt.goready.scanner.DebugScanView
import com.tomcvt.goready.scanner.SaveBarcodeScreen
import com.tomcvt.goready.viewmodel.AlarmViewModel
import com.tomcvt.goready.viewmodel.SettingsViewModel

private enum class SettingsTab { GENERAL, BLUETOOTH }

@Composable
fun SettingsView(
    viewModel: SettingsViewModel,
    deviceScanViewModel: DeviceScanViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(SettingsTab.GENERAL) }
    val showBleScenariosModal by remember { deviceScanViewModel.showBleScenariosModal }.collectAsState()

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { selectedTab = SettingsTab.GENERAL }) { Text("Settings") }
            Button(onClick = { selectedTab = SettingsTab.BLUETOOTH }) { Text("Bluetooth") }
        }

        when (selectedTab) {
            SettingsTab.GENERAL -> PermissionSettingsScreen(viewModel = viewModel, deviceScanViewModel = deviceScanViewModel)
            SettingsTab.BLUETOOTH -> ScanScreen(viewModel = deviceScanViewModel, onSelect = {})
        }
    }
}

@Composable
fun PermissionSettingsScreen(
    viewModel: SettingsViewModel,
    deviceScanViewModel: DeviceScanViewModel,
    modifier: Modifier = Modifier
) {
    val mainContext = LocalContext.current

    val premiumState by viewModel.premiumState.collectAsState()
    //just false or true
    var showDebugModal by remember { mutableStateOf(false) }


    val permissionStateMap = remember {
        mutableStateMapOf<String, Boolean>()
    }

    val permissionRegistry = remember { getPermissionRegistryForSdk(Build.VERSION.SDK_INT) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        refreshPermissionState(mainContext, permissionRegistry, permissionStateMap)
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissionState(mainContext, permissionRegistry, permissionStateMap)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        refreshPermissionState(mainContext, permissionRegistry, permissionStateMap)
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            PermissionList(
                registry = permissionRegistry,
                stateMap = permissionStateMap,
                onOptionClick = { id ->
                    val spec = permissionRegistry.find { it.id == id }
                    if (spec != null) {
                        requestPermission(mainContext, spec, launcher)
                    }
                }
            )
            if (permissionStateMap["battery_optimization"] == false) {
                Text(text = "Battery optimization is not granted")
            }
            if (BuildConfig.DEBUG) {
                Button(onClick = {
                    viewModel.devTogglePremium()
                }) {
                    Text(text = "Premium: ${premiumState.isPremium}")
                }
            }
            if (BuildConfig.DEBUG) {
                Button(onClick = {
                    showDebugModal = true
                }) {
                    Text(text = "Show debug modal")
                }
            }
        }
        if (showDebugModal) {
            SettingsDebugMenu(deviceScanViewModel = deviceScanViewModel, onDismiss = { showDebugModal = false })
        }
    }
}

fun refreshPermissionState(
    context: Context,
    permissionRegistry: List<PermissionSpec>,
    permissionStateMap: MutableMap<String, Boolean>
) {
    permissionRegistry.forEach { spec ->
        val granted = if (Build.VERSION.SDK_INT >= spec.minSdk) {
            ContextCompat.checkSelfPermission(
                context,
                spec.permission
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        permissionStateMap[spec.id] = granted
    }
}

fun requestPermission(context: Context, spec: PermissionSpec,
                      launcher: ManagedActivityResultLauncher<String, Boolean>) {
    if (spec.id == "battery_optimization") {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        val packageName = context.packageName
        intent.data = Uri.parse("package:$packageName")
        context.startActivity(intent)
    } else {
        //TODO make compatible
        //ActivityCompat.requestPermissions(context as Activity, arrayOf(spec.permission), spec.callbackInt)
        val packageName = context.packageName
        launcher.launch(spec.permission)
    }
}

@Composable
fun PermissionList(
    registry: List<PermissionSpec>,
    stateMap: Map<String, Boolean>,
    onOptionClick: (String) -> Unit
) {
    Column {
        registry.forEach { spec ->
            PermissionRow(
                id = spec.id,
                description = spec.description,
                granted = stateMap[spec.id] == true,
                onClick = { onOptionClick(spec.id) }
            )
        }
    }
}

@Composable
fun PermissionRow(
    id: String,
    description: String,
    granted: Boolean,
    onClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(id) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = description)

        Icon(
            imageVector = if (granted) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (granted) Color.Green else Color.Red
        )
    }
}


//TODO migrate to /debug/ and remove content in production
@Composable
fun SettingsDebugMenu(
    deviceScanViewModel: DeviceScanViewModel,
    onDismiss: () -> Unit
) {
    var showBleScenarios by remember { mutableStateOf(false) }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var showSaveBarcode by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BackHandler(onBack = onDismiss)
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Debug Menu", style = MaterialTheme.typography.headlineSmall)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                HorizontalDivider()
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        DebugMenuRow(
                            title = "BLE Test Scenarios",
                            description = "Run BLE connection and command test scenarios"
                        ) { showBleScenarios = true }
                    }
                    item {
                        DebugMenuRow(
                            title = "Barcode Scanner Test",
                            description = "Build a code list and test the multi-barcode scanner"
                        ) { showBarcodeScanner = true }
                    }
                    item {
                        DebugMenuRow(
                            title = "Save Barcode Screen",
                            description = "Test the single barcode capture screen"
                        ) { showSaveBarcode = true }
                    }
                }
            }
        }
    }

    if (showBleScenarios) {
        BleScenariosModal(
            viewModel = deviceScanViewModel,
            onDismiss = { showBleScenarios = false }
        )
    }

    if (showBarcodeScanner) {
        Dialog(
            onDismissRequest = { showBarcodeScanner = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            BackHandler { showBarcodeScanner = false }
            DebugScanView(
                onDismiss = { showBarcodeScanner = false },
                onFinished = { showBarcodeScanner = false }
            )
        }
    }

    if (showSaveBarcode) {
        Dialog(
            onDismissRequest = { showSaveBarcode = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            BackHandler { showSaveBarcode = false }
            SaveBarcodeScreen(
                onSaved = { showSaveBarcode = false },
                onDismissed = { showSaveBarcode = false }
            )
        }
    }
}

@Composable
private fun DebugMenuRow(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

