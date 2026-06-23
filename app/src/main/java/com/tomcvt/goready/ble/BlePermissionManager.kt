package com.tomcvt.goready.ble

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class BlePermissionState(
    val canScan: Boolean,
    val canConnect: Boolean,
    val locationServicesOn: Boolean // n/a (true) on API 31+, real check below that
) {
    val isReady: Boolean get() = canScan && canConnect && locationServicesOn
}

class BlePermissionManager(private val appContext: Context) {

    private val _state = MutableStateFlow(computeState())
    val state: StateFlow<BlePermissionState> = _state

    fun refresh() {
        _state.value = computeState()
    }

    /** What to hand a RequestMultiplePermissions launcher. */
    fun permissionsToRequest(): Array<String> = if (Build.VERSION.SDK_INT >= 31) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    fun needsLocationServicesPrompt(): Boolean = Build.VERSION.SDK_INT < 31

    fun openLocationSettings() {
        appContext.startActivity(
            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun computeState(): BlePermissionState = if (Build.VERSION.SDK_INT >= 31) {
        BlePermissionState(
            canScan = granted(Manifest.permission.BLUETOOTH_SCAN),
            canConnect = granted(Manifest.permission.BLUETOOTH_CONNECT),
            locationServicesOn = true // neverForLocation — not required
        )
    } else {
        BlePermissionState(
            canScan = granted(Manifest.permission.ACCESS_FINE_LOCATION),
            canConnect = true, // BLUETOOTH is a normal permission pre-31, already granted
            locationServicesOn = isLocationServicesEnabled(appContext)
        )
    }

    private fun granted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED
}

private fun isLocationServicesEnabled(context: Context): Boolean {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return if (Build.VERSION.SDK_INT >= 28) {
        lm.isLocationEnabled
    } else {
        Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF) != Settings.Secure.LOCATION_MODE_OFF
    }
}