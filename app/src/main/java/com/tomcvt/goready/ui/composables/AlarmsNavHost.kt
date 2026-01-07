package com.tomcvt.goready.ui.composables

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tomcvt.goready.viewmodel.AlarmViewModel
import com.tomcvt.goready.viewmodel.AlarmViewModelFactory

@Composable
fun AlarmsNavHost(
    factory: AlarmViewModelFactory
) {
    val navController = rememberNavController()

    NavHost(navController, startDestination = "list") {
        composable("list") {
            val vm: AlarmViewModel = viewModel(factory = factory)
            AlarmListRoute(vm, navController)
        }
        composable("add") {
            val vm: AlarmViewModel = viewModel(factory = factory)
            AddAlarmRoute(vm)
        }
    }
}