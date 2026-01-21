package com.tomcvt.goready.viewmodel

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.retain.RetainedValuesStoreRegistry
import androidx.core.content.ContextCompat.startActivity
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomcvt.goready.constants.TaskType
import com.tomcvt.goready.data.AlarmEntity
import com.tomcvt.goready.domain.AlarmDraft
import com.tomcvt.goready.domain.SimpleAlarmDraft
import com.tomcvt.goready.manager.AppAlarmManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek

class AlarmViewModel(
    private val appAlarmManager: AppAlarmManager // inject manager
) : ViewModel() {
    private val TAG = "AlarmViewModel"

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    private val _editorState =
        MutableStateFlow(AlarmEditorState())

    val editorState: StateFlow<AlarmEditorState> =
        _editorState.asStateFlow()

    private val _rememberedData = MutableStateFlow<MutableMap<String, String>>(mutableMapOf())

    val rememberedData: StateFlow<MutableMap<String, String>> =
        _rememberedData.asStateFlow()

    val alarmsStateFlow: StateFlow<List<AlarmEntity>> = appAlarmManager
        .getAlarmsFlow().stateIn(
            viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(1000),
            initialValue = emptyList()
        )

    private var currentAlarmId: Long? = null

    fun initEditor(alarmId: Long?) {
        //if (currentAlarmId == alarmId) return

        currentAlarmId = alarmId

        if (alarmId == null) {
            _editorState.value = AlarmEditorState(mode = AlarmEditorState.Mode.CREATE)
        } else {
            viewModelScope.launch {
                val alarm = appAlarmManager.getAlarm(alarmId)?: return@launch
                _editorState.value = AlarmEditorState(
                    mode = AlarmEditorState.Mode.EDIT,
                    hour = alarm.hour,
                    minute = alarm.minute,
                    repeatDays = alarm.repeatDays,
                    taskType = TaskType.valueOf(alarm.task?: "NONE"),
                    taskData = alarm.taskData?: ""
                )
                TaskType.values().forEach {
                    if (it.name == alarm.task) {
                        _rememberedData.value[it.name] = alarm.taskData?: ""
                    } else {
                        _rememberedData.value[it.name] = ""
                    }
                }
                Log.d("AlarmViewModel", "Alarm loaded: $alarm")
            }
        }
    }

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

    fun loadForEdit(alarmId: Long) {
        viewModelScope.launch {
            val alarm = appAlarmManager.getAlarm(alarmId)?: return@launch
            _editorState.value = AlarmEditorState(
                mode = AlarmEditorState.Mode.EDIT,
                hour = alarm.hour,
                minute = alarm.minute,
                repeatDays = alarm.repeatDays,
                taskType = TaskType.valueOf(alarm.task?: "NONE"),
                taskData = alarm.taskData?: ""
            )
        }
    }

    fun startNew() {
        _editorState.value = AlarmEditorState()
    }

    fun save() {
        viewModelScope.launch {
            val s = editorState.value
            if (s.mode == AlarmEditorState.Mode.CREATE) {
                val draft = AlarmDraft(
                    hour = s.hour,
                    minute = s.minute,
                    repeatDays = s.repeatDays,
                    task = s.taskType.name,
                    taskData = s.taskData,
                    snoozeDurationMinutes = s.snoozeTime,
                    snoozeMaxCount = s.snoozeCount,
                    snoozeEnabled = s.snoozeActive
                )
                Log.d(TAG, "Saving alarmDraft: $draft")
                appAlarmManager.createAlarm(draft)
                _uiState.value = UiState.Success("Alarm saved")
            } else {
                appAlarmManager.updateAlarm(AlarmDraft(
                    hour = s.hour,
                    minute = s.minute,
                    repeatDays = s.repeatDays,
                    task = s.taskType.name,
                    taskData = s.taskData,
                    snoozeDurationMinutes = s.snoozeTime,
                    snoozeMaxCount = s.snoozeCount,
                    snoozeEnabled = s.snoozeActive
                ), currentAlarmId?: return@launch)
                _uiState.value = UiState.Success("Alarm updated")
            }
        }
    }

    fun setTime(hour: Int, minute: Int) {
        _editorState.update {
            it.copy(hour = hour, minute = minute)
        }
    }

    fun toggleDay(day: DayOfWeek) {
        _editorState.update {
            val newDays =
                if (day in it.repeatDays) it.repeatDays - day
                else it.repeatDays + day
            it.copy(repeatDays = newDays)
        }
    }

    fun setTaskType(type: TaskType) {
        _editorState.update { it.copy(taskType = type) }
    }

    fun setTaskData(data: String) {
        val data = parseData(editorState.value.taskType, data)
        if (data == null) {
            _uiState.value = UiState.Error("Invalid data")
            return
        }
        _editorState.update { it.copy(taskData = data) }
        _rememberedData.update {
            it[editorState.value.taskType.name] = data
            it
        }
    }

    fun setSnoozeActive(active: Boolean) {
        _editorState.update {
            it.copy(snoozeActive = active)
        }
    }

    fun setSnoozeTime(time: Int) {
        _editorState.update {
            it.copy(snoozeTime = time)
        }
    }

    fun setSnoozeCount(count: Int) {
        _editorState.update {
            it.copy(snoozeCount = count)
        }
    }
}

// UI observes `uiState` and reacts
sealed class UiState {
    object Idle: UiState()
    data class Success(val message: String): UiState()
    data class Error(val message: String): UiState()
}

data class AlarmEditorState(
    val mode: Mode = Mode.CREATE,
    val hour: Int = 8,
    val minute: Int = 30,
    val repeatDays: Set<DayOfWeek> = emptySet(),
    val taskType: TaskType = TaskType.NONE,
    val taskData: String = "",
    val isLoading: Boolean = false,
    val snoozeTime: Int = 5,
    val snoozeActive: Boolean = false,
    val snoozeCount: Int = 1
) {
    enum class Mode { CREATE, EDIT }
}


fun parseData(taskType: TaskType, taskData: String) : String?  {

    when (taskType) {
        TaskType.TIMER -> {return null}
        TaskType.COUNTDOWN -> {
            if (taskData.isDigitsOnly() && taskData.toInt() > 0) {
                return taskData
            }
            return null
        }
        TaskType.TEXT -> {
            if (taskData.isNotBlank()) {
                return taskData
            }
            return null
        }
        TaskType.MATH -> {return taskData}
        else -> {return null}
    }
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





