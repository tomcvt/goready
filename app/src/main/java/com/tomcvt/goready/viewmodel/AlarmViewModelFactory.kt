package com.tomcvt.goready.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tomcvt.goready.manager.AppAlarmManager

class AlarmViewModelFactory(private val appAlarmManager: AppAlarmManager
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlarmViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AlarmViewModel(appAlarmManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
