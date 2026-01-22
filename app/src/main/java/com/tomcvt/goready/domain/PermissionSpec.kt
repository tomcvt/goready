package com.tomcvt.goready.domain

import android.os.Build

data class PermissionSpec(
    val id: String,
    val label: String,
    val description: String,
    val permission: String,
    val minSdk: Int,
    val maxSdkVersion: Int = Build.VERSION.SDK_INT,
    val callbackInt: Int = 0
)
