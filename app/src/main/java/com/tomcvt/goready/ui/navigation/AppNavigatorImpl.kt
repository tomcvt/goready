package com.tomcvt.goready.ui.navigation

import com.tomcvt.goready.AppDestinations
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppNavigatorImpl(start: AppDestinations) : AppNavigator {
    private val _current = MutableStateFlow(start)
    override val current = _current.asStateFlow()
    override fun navigateTo(destination: AppDestinations) {
        _current.value = destination
    }
}
