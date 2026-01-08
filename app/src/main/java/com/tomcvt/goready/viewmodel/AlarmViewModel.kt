package com.tomcvt.goready.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomcvt.goready.data.AlarmEntity
import com.tomcvt.goready.domain.AlarmDraft
import com.tomcvt.goready.domain.SimpleAlarmDraft
import com.tomcvt.goready.manager.AlarmManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmViewModel(
    private val alarmManager: AlarmManager // inject manager
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    val alarmsStateFlow: StateFlow<List<AlarmEntity>> = alarmManager
        .getAlarmsFlow().stateIn(
            viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(1000),
            initialValue = emptyList()
        )

    fun saveAlarm(draft: AlarmDraft) {
        viewModelScope.launch {
            try {
                alarmManager.createAlarm(draft)
                Log.d("AlarmViewModel", "Alarm created: $draft")
                _uiState.value = UiState.Success("Alarm saved")
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun saveSimpleAlarm(draft: SimpleAlarmDraft) {
        viewModelScope.launch {
            try {
                alarmManager.createSimpleAlarm(draft)
                Log.d("AlarmViewModel", "Simple alarm created: $draft")
                _uiState.value = UiState.Success("Simple alarm saved")
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun toggleAlarm(alarm: AlarmEntity, enabled: Boolean) {
        viewModelScope.launch {
            try {
                alarmManager.toggleAlarm(alarm, enabled)
                _uiState.value = UiState.Success("Alarm toggled")
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            try {
                alarmManager.deleteAlarm(alarm)
                Log.d("AlarmViewModel", "Alarm deleted: $alarm")
                _uiState.value = UiState.Success("Alarm deleted")
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
