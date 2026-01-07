package com.tomcvt.goready.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.tomcvt.goready.AppDestinations
import com.tomcvt.goready.Greeting
import com.tomcvt.goready.ui.composables.AddAlarmView
import com.tomcvt.goready.ui.composables.AlarmList
import com.tomcvt.goready.ui.composables.HomeScreen


@Composable
fun RootContent(modifier: Modifier = Modifier) {
    val root = LocalRootNavigator.current
    val currentTab by root.currentTab.collectAsState()

    when (currentTab) {
        RootTab.HOME -> RootTabHost(rootTab = RootTab.HOME, nav = root.navigatorFor(RootTab.HOME), modifier = Modifier)
        RootTab.ALARMS -> RootTabHost(rootTab = RootTab.ALARMS, nav = root.navigatorFor(RootTab.ALARMS), modifier = Modifier)
        RootTab.ADD_ALARM -> RootTabHost(rootTab = RootTab.ADD_ALARM, nav = root.navigatorFor(RootTab.ADD_ALARM), modifier = Modifier)
        RootTab.SETTINGS -> RootTabHost(rootTab = RootTab.SETTINGS, nav = root.navigatorFor(RootTab.SETTINGS), modifier = Modifier)
    }
}

@Composable
fun RootTabHost(rootTab: RootTab, nav: AppNavigator, modifier: Modifier = Modifier) {
    val current by nav.current.collectAsState(initial = AppDestinations.HOME)

    when (current) {
        AppDestinations.HOME -> HomeScreen(modifier = modifier)
        //AppDestinations.ALARMS -> AlarmList(modifier = modifier)
        //AppDestinations.ADD_ALARM -> AddAlarmView(modifier = modifier)
        AppDestinations.SETTINGS -> Greeting("Settings")
        AppDestinations.ALARMS -> Greeting("hello")
        AppDestinations.ADD_ALARM -> Greeting("hello")

    }
}