package com.tomcvt.goready.domain

data class PermissionSpec(
    val id: String,
    val label: String,
    val description: String,
    val permission: String,
    val minSdk: Int,
    val maxSdk: Int = Int.MAX_VALUE,
    val callbackInt: Int = 0
)