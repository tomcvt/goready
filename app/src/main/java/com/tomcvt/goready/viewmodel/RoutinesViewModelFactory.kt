package com.tomcvt.goready.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tomcvt.goready.manager.AppRoutinesManager

class RoutinesViewModelFactory(private val routinesManager: AppRoutinesManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RoutinesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RoutinesViewModel(routinesManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}