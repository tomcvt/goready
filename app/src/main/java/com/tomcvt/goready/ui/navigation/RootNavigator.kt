package com.tomcvt.goready.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.flow.StateFlow

enum class RootTab(val label: String,
                   val icon: ImageVector,
) {
    HOME("Home", Icons.Default.Home),
    ALARMS("Alarms", Icons.Default.Home),
    SETTINGS("Profile", Icons.Default.Settings),
    ADD_ALARM("Add Alarm", Icons.Default.AddCircle);
}

interface RootNavigator {
    val currentTab: StateFlow<RootTab>
    fun switchTab(rootTab: RootTab)
    fun navigatorFor(rootTab: RootTab): AppNavigator
}

val LocalRootNavigator = staticCompositionLocalOf<RootNavigator> { error("No RootNavigator provided") }