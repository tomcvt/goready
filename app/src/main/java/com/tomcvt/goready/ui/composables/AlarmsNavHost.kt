package com.tomcvt.goready.ui.composables

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tomcvt.goready.viewmodel.AlarmViewModel
import com.tomcvt.goready.viewmodel.AlarmViewModelFactory

@Composable
fun AlarmsNavHost(
    factory: AlarmViewModelFactory,
    rootController: NavHostController,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    var showExitModal by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        showExitModal = true
    }

    Box (
        modifier = modifier
    ) {
        NavHost(navController, startDestination = "list") {
            composable("list") {
                val vm: AlarmViewModel = viewModel(factory = factory)
                AlarmListRoute(vm, navController, rootController)
            }
        }
        if (showExitModal) {
            StandardModal(
                onDismiss = { showExitModal = false },
                onConfirm = { rootController.popBackStack() }
            ) {
                //TODO square ad banner
                Text("Do you want to exit?")
            }
        }
    }

}