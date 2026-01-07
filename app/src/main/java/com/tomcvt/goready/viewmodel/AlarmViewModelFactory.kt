package com.tomcvt.goready.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tomcvt.goready.manager.AlarmManager

class AlarmViewModelFactory(private val alarmManager: AlarmManager
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlarmViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AlarmViewModel(alarmManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
