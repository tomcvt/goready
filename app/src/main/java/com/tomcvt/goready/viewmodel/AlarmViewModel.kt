package com.tomcvt.goready.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomcvt.goready.domain.AlarmDraft
import com.tomcvt.goready.manager.AlarmManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AlarmViewModel(
    private val alarmManager: AlarmManager // inject manager
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    fun saveAlarm(draft: AlarmDraft) {
        viewModelScope.launch {
            try {
                alarmManager.createAlarm(draft)
                _uiState.value = UiState.Success("Alarm saved")
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

// UI observes `uiState` and reacts
sealed class UiState {
    object Idle: UiState()
    data class Success(val message: String): UiState()
    data class Error(val message: String): UiState()
}