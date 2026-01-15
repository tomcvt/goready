package com.tomcvt.goready.ui.composables

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tomcvt.goready.viewmodel.AlarmViewModel
import com.tomcvt.goready.viewmodel.PermissionSpec
import com.tomcvt.goready.viewmodel.permissionRegistry

@Composable
fun SettingsView(modifier: Modifier = Modifier) {
    PermissionSettingsScreen()
}

@Composable
fun PermissionSettingsScreen() {
    val context = LocalContext.current

    val permissionStateMap = remember {
        mutableStateMapOf<String, Boolean>()
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        refreshPermissionState(context, permissionStateMap)
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissionState(context, permissionStateMap)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        refreshPermissionState(context, permissionStateMap)
    }

    PermissionList(
        registry = permissionRegistry,
        stateMap = permissionStateMap,
        onOptionClick = { id ->
            val spec = permissionRegistry.find { it.id == id }
            if (spec != null) {
                requestPermission(context, spec, launcher)
            }
        }
    )
}

fun refreshPermissionState(
    context: Context,
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

