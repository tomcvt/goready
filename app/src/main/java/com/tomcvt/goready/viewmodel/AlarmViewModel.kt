package com.tomcvt.goready.viewmodel

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.retain.RetainedValuesStoreRegistry
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomcvt.goready.data.AlarmEntity
import com.tomcvt.goready.domain.AlarmDraft
import com.tomcvt.goready.domain.SimpleAlarmDraft
import com.tomcvt.goready.manager.AppAlarmManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmViewModel(
    private val appAlarmManager: AppAlarmManager // inject manager
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    val alarmsStateFlow: StateFlow<List<AlarmEntity>> = appAlarmManager
        .getAlarmsFlow().stateIn(
            viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(1000),
            initialValue = emptyList()
        )

    fun saveAlarm(draft: AlarmDraft) {
        viewModelScope.launch {
            try {
                appAlarmManager.createAlarm(draft)
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
                appAlarmManager.createSimpleAlarm(draft)
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
                appAlarmManager.toggleAlarm(alarm, enabled)
                _uiState.value = UiState.Success("Alarm toggled")
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            try {
                appAlarmManager.deleteAlarm(alarm)
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

data class PermissionSpec(
    val id: String,
    val label: String,
    val description: String,
    val permission: String,
    val minSdk: Int,
    val callbackInt: Int = 0
)

val permissionRegistry = listOf(
    PermissionSpec(
        id = "exact_alarm",
        label = "Exact Alarm",
        description = "Schedule exact alarms",
        permission = Manifest.permission.SCHEDULE_EXACT_ALARM,
        minSdk = Build.VERSION_CODES.S,
        callbackInt = 103),
    PermissionSpec(
        id = "foreground_service",
        label = "Foreground Service",
        description = "Alarm Manager",
        permission = Manifest.permission.FOREGROUND_SERVICE,
        minSdk = Build.VERSION_CODES.Q,
        callbackInt = 102),
    PermissionSpec(
        id = "full_screen_intent",
        label = "Full Screen Intent",
        description = "Allow full screen alarm",
        permission = Manifest.permission.USE_FULL_SCREEN_INTENT,
        minSdk = Build.VERSION_CODES.Q,
        callbackInt = 104),
    PermissionSpec(
        id = "post_notifications",
        label = "Post Notifications",
        description = "Posting notifications",
        permission = Manifest.permission.POST_NOTIFICATIONS,
        minSdk = Build.VERSION_CODES.TIRAMISU,
        callbackInt = 101),
    PermissionSpec(
        id = "battery_optimization",
        label = "Battery Optimization",
        description = "Allow app to be excluded from battery optimization",
        permission = Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        minSdk = Build.VERSION_CODES.M,
        callbackInt = 107),
    PermissionSpec(
        id = "foreground_service_system_exempt",
        label = "Foreground Service System Exempt",
        description = "Allow app to have special privilige to run reliably",
        permission = Manifest.permission.FOREGROUND_SERVICE_SYSTEM_EXEMPTED,
        minSdk = Build.VERSION_CODES.Q,
        callbackInt = 105),
    PermissionSpec(
        id = "foreground_service_media_playback",
        label = "Foreground Service Media Playback",
        description = "Allow app to sound alarms",
        permission = Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK,
        minSdk = Build.VERSION_CODES.Q,
        callbackInt = 106),
    PermissionSpec(
        id = "use_exact_alarm",
        label = "Use Exact Alarm",
        description = "Use exact alarm",
        permission = Manifest.permission.USE_EXACT_ALARM,
        minSdk = Build.VERSION_CODES.TIRAMISU,
        callbackInt = 108)
    )





