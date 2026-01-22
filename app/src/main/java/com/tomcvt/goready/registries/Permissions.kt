package com.tomcvt.goready.registries

import android.Manifest
import android.os.Build
import com.tomcvt.goready.domain.PermissionSpec

fun getPermissionListForSdk(sdk: Int): List<PermissionSpec> {
    return permissionRegistry.filter { it.minSdk <= sdk && it.maxSdkVersion >= sdk }
}

private val permissionRegistry = listOf(
    PermissionSpec(
        id = "exact_alarm",
        label = "Exact Alarm",
        description = "Schedule exact alarms",
        permission = Manifest.permission.SCHEDULE_EXACT_ALARM,
        minSdk = Build.VERSION_CODES.S,
        maxSdkVersion = 32,
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
/*
Old code (bad)
val permissionRegistry = listOf(
    PermissionSpec(
        id = "exact_alarm",
        label = "Exact Alarm",
        description = "Schedule exact alarms",
        permission = Manifest.permission.SCHEDULE_EXACT_ALARM,
        minSdk = Build.VERSION_CODES.S,
        callbackInt = 103
    ),
    PermissionSpec(
        id = "foreground_service",
        label = "Foreground Service",
        description = "Alarm Manager",
        permission = Manifest.permission.FOREGROUND_SERVICE,
        minSdk = Build.VERSION_CODES.Q,
        callbackInt = 102),
    PermissionSpec(
        id = "full_screen_intent",
        label = "Full Screen Intent",
        description = "Allow full screen alarm",
        permission = Manifest.permission.USE_FULL_SCREEN_INTENT,
        minSdk = Build.VERSION_CODES.Q,
        callbackInt = 104),
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
    PermissionSpec(
        id = "foreground_service_system_exempt",
        label = "Foreground Service System Exempt",
        description = "Allow app to have special privilige to run reliably",
        permission = Manifest.permission.FOREGROUND_SERVICE_SYSTEM_EXEMPTED,
        minSdk = Build.VERSION_CODES.Q,
        callbackInt = 105),
    PermissionSpec(
        id = "foreground_service_media_playback",
        label = "Foreground Service Media Playback",
        description = "Allow app to sound alarms",
        permission = Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK,
        minSdk = Build.VERSION_CODES.Q,
        callbackInt = 106),
    PermissionSpec(
        id = "use_exact_alarm",
        label = "Use Exact Alarm",
        description = "Use exact alarm",
        permission = Manifest.permission.USE_EXACT_ALARM,
        minSdk = Build.VERSION_CODES.TIRAMISU,
        callbackInt = 108)
    )
 */