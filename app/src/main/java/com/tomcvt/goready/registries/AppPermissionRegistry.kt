package com.tomcvt.goready.registries

import android.Manifest
import android.os.Build
import com.tomcvt.goready.domain.PermissionSpec

fun getPermissionRegistryForSdk(sdk: Int): List<PermissionSpec> {
    return permissionRegistry.filter { it.minSdk <= sdk && it.maxSdk >= sdk }
}

val permissionRegistry = listOf(
    PermissionSpec(
        id = "exact_alarm",
        label = "Exact Alarm",
        description = "Schedule exact alarms",
        permission = Manifest.permission.SCHEDULE_EXACT_ALARM,
        minSdk = Build.VERSION_CODES.S,
        maxSdk = Build.VERSION_CODES.TIRAMISU,
        callbackInt = 103),
    PermissionSpec(
        id = "post_notifications",
        label = "Post Notifications",
        description = "Posting notifications",
        permission = Manifest.permission.POST_NOTIFICATIONS,
        minSdk = Build.VERSION_CODES.TIRAMISU,
        callbackInt = 101),
    PermissionSpec(
        id = "battery_optimization",
        label = "Battery Optimization",
        description = "Allow app to be excluded from battery optimization",
        permission = Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        minSdk = Build.VERSION_CODES.M,
        callbackInt = 107),
)