package com.tomcvt.goready.ui.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.StateFlow
import com.tomcvt.goready.AppDestinations

interface AppNavigator {
    val current: StateFlow<AppDestinations>
    fun navigateTo(destination: AppDestinations)
}

val LocalAppNavigator = staticCompositionLocalOf<AppNavigator> {
    error("No AppNavigator provided")
}