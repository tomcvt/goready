package com.tomcvt.goready.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tomcvt.goready.manager.RoutineFlowManager

class RoutineFlowViewModelFactory(
    private val routineManager: RoutineFlowManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RoutineFlowViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RoutineFlowViewModel(routineManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")

    }
}