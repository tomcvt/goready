package com.tomcvt.goready.ui.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.tomcvt.goready.AppDestinations

class RootNavigatorImpl(start: RootTab = RootTab.HOME) : RootNavigator {
    private val _currentTab = MutableStateFlow(start)
    override val currentTab = _currentTab.asStateFlow()

    private val navMap: Map<RootTab, AppNavigator> = RootTab.values().associateWith { tab ->
        AppNavigatorImpl(
            start = when (tab) {
                RootTab.HOME -> AppDestinations.HOME
                RootTab.ALARMS -> AppDestinations.ALARMS
                RootTab.ADD_ALARM -> AppDestinations.ADD_ALARM
                RootTab.SETTINGS -> AppDestinations.SETTINGS
            }
        )
    }

    override fun switchTab(rootTab: RootTab) { _currentTab.value = rootTab }
    override fun navigatorFor(rootTab: RootTab): AppNavigator = navMap.getValue(rootTab)
}