@file:OptIn(ExperimentalCoroutinesApi::class)

package com.tomcvt.goready.viewmodel

import android.util.Log
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomcvt.goready.ble.BleDeviceManager
import com.tomcvt.goready.constants.MathType
import com.tomcvt.goready.constants.TaskType
import com.tomcvt.goready.data.AlarmEntity
import com.tomcvt.goready.data.RoutineEntity
import com.tomcvt.goready.data.StepWithDefinition
import com.tomcvt.goready.domain.AlarmDraft
import com.tomcvt.goready.manager.AppAlarmManager
import com.tomcvt.goready.manager.AppAlarmManagerImpl
import com.tomcvt.goready.manager.AppRoutinesManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek

private const val TAG = "AlarmViewModel"
class AlarmViewModel(
    private val appAlarmManager: AppAlarmManager,
    private val routinesManager: AppRoutinesManager
) : ViewModel() {
    //private val syncPrefs =
    private val _uiState = MutableStateFlow<UiState>(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private val _editorState =
        MutableStateFlow(AlarmEditorState())

    val editorState: StateFlow<AlarmEditorState> =
        _editorState.asStateFlow()

    private val _rememberedData = MutableStateFlow<MutableMap<String, String>>(mutableMapOf())

    val rememberedData: StateFlow<MutableMap<String, String>> =
        _rememberedData.asStateFlow()

    private val _selectedRoutineId = MutableStateFlow<Long?>(null)
    val selectedRoutineId: StateFlow<Long?> =
        _selectedRoutineId.asStateFlow()
    private val _previewRoutineId = MutableStateFlow<Long?>(null)
    val previewRoutineId: StateFlow<Long?> =
        _previewRoutineId.asStateFlow()

    val previewRoutineSteps: StateFlow<List<StepWithDefinition>> =
        previewRoutineId.filterNotNull()
            .flatMapLatest { id ->
                routinesManager.getRoutineStepsWithDefinitionFlow(id)
            }
            .stateIn(
                viewModelScope,
                started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(1000),
                initialValue = emptyList()
            )

    val previewRoutineEntity: StateFlow<RoutineEntity?> =
        previewRoutineId.filterNotNull()
            .flatMapLatest { id ->
                routinesManager.getRoutineByIdFlow(id)
            }.filterNotNull()
            .stateIn(
                viewModelScope,
                started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(1000),
                initialValue = null
            )

    val routinesStateFlow: StateFlow<List<RoutineEntity>> =
        routinesManager.getAllRoutinesFlow().stateIn(
            viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(1000),
            initialValue = emptyList()
        )

    val alarmsStateFlow: StateFlow<List<AlarmEntity>> = appAlarmManager
        .getAlarmsFlow().stateIn(
            viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(1000),
            initialValue = emptyList()
        )

    private var currentAlarmId: Long? = null

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncStatus: MutableStateFlow<BleDeviceManager.SyncResult?> = MutableStateFlow(null)
    val syncStatus: StateFlow<BleDeviceManager.SyncResult?> = _syncStatus.asStateFlow()

    val lastSyncTime: StateFlow<Long?> = appAlarmManager.lastSyncEpochSeconds.stateIn(
        viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(1000),
        initialValue = null
    )


    /**
     * Pushes a fresh full sync of all ESP-enabled alarms to the connected BLE device.
     * Progress/result is exposed via [isSyncing] and [syncStatus] for the UI to render.
     */
    fun forceFullSync() {
        if (_isSyncing.value) return
        _isSyncing.value = true
        _syncStatus.value = null
        viewModelScope.launch {
            try {
                _syncStatus.value = appAlarmManager.syncAllEspEnabledAlarms()
            } catch (e: Exception) {
                Log.e(TAG, "ESP alarm sync failed", e)
                _syncStatus.value = BleDeviceManager.SyncResult.Error(e.message ?: "Unknown error")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun dismissSyncStatus() {
        _syncStatus.value = null
    }

    fun initEditor(alarmId: Long?) {
        //if (currentAlarmId == alarmId) return

        currentAlarmId = alarmId

        if (alarmId == null || alarmId == 0L) {
            _editorState.value = AlarmEditorState(mode = AlarmEditorState.Mode.CREATE)
        } else {
            viewModelScope.launch {
                val alarm = appAlarmManager.getAlarm(alarmId)?: return@launch
                _editorState.value = AlarmEditorState(
                    mode = AlarmEditorState.Mode.EDIT,
                    hour = alarm.hour,
                    minute = alarm.minute,
                    repeatDays = alarm.repeatDays,
                    taskType = alarm.task,
                    taskData = alarm.taskData?: "",
                    snoozeTime = alarm.snoozeDurationMinutes?: 0,
                    snoozeCount = alarm.snoozeMaxCount?: 0,
                    snoozeActive = alarm.snoozeEnabled,
                    routineId = alarm.routineId,
                    espEnabled = alarm.espEnabled
                )
                TaskType.values().forEach {
                    if (it.name == alarm.task.name) {
                        _rememberedData.value[it.name] = alarm.taskData?: ""
                    } else {
                        _rememberedData.value[it.name] = ""
                    }
                }
                Log.d("AlarmViewModel", "Alarm loaded: $alarm")
            }
        }
    }

    fun toggleAlarm(alarm: AlarmEntity, enabled: Boolean) {
        viewModelScope.launch {
            try {
                appAlarmManager.toggleAlarm(alarm, enabled)
                _uiState.update { it.copy(successMessage = "Alarm toggled") }
                // TODO later add singular sync push if didn't disconnect since last sync
                forceFullSync()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Unknown error") }
            }
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            try {
                appAlarmManager.deleteAlarm(alarm)
                Log.d("AlarmViewModel", "Alarm deleted: $alarm")
                _uiState.update { it.copy(successMessage = "Alarm deleted") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Unknown error") }
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
                taskType = alarm.task,
                taskData = alarm.taskData?: ""
            )
        }
    }

    fun startNew() {
        _editorState.value = AlarmEditorState()
    }

    fun save() {
        viewModelScope.launch {
            var s = editorState.value
            if (!validateData(s.taskType, s.taskData)) {
                _uiState.update { it.copy(inputErrorMessage = "Invalid input data") }
                return@launch
            }
            s = s.copy(taskData = trimTaskData(s.taskType, s.taskData))
            if (s.mode == AlarmEditorState.Mode.CREATE) {
                val draft = AlarmDraft(
                    hour = s.hour,
                    minute = s.minute,
                    repeatDays = s.repeatDays,
                    task = s.taskType,
                    taskData = s.taskData,
                    snoozeDurationMinutes = s.snoozeTime,
                    snoozeMaxCount = s.snoozeCount,
                    snoozeEnabled = s.snoozeActive,
                    routineId = s.routineId,
                    espEnabled = s.espEnabled
                )
                Log.d(TAG, "Saving alarmDraft: $draft")
                appAlarmManager.createAlarm(draft)
                _uiState.update { it.copy(successMessage = "Alarm created") }
                // TODO later add singular sync push if didn't disconnect since last sync
                forceFullSync()
            } else {
                appAlarmManager.updateAlarm(AlarmDraft(
                    hour = s.hour,
                    minute = s.minute,
                    repeatDays = s.repeatDays,
                    task = s.taskType,
                    taskData = s.taskData,
                    snoozeDurationMinutes = s.snoozeTime,
                    snoozeMaxCount = s.snoozeCount,
                    snoozeEnabled = s.snoozeActive,
                    routineId = s.routineId,
                    espEnabled = s.espEnabled
                ), currentAlarmId?: return@launch)
                _uiState.update { it.copy(successMessage = "Alarm updated") }
                // TODO later add singular sync push if didn't disconnect since last sync
                forceFullSync()
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
            _uiState.update { it.copy(inputErrorMessage = "Invalid input data") }
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

    fun setEspEnabled(enabled: Boolean) {
        _editorState.update {
            it.copy(espEnabled = enabled)
        }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearInputErrorMessage() {
        _uiState.update { it.copy(inputErrorMessage = null) }
    }

    fun openRoutineSelector() {
        _uiState.update { it.copy(routineSelectorOpen = true) }
    }

    fun closeRoutineSelector() {
        _uiState.update { it.copy(routineSelectorOpen = false) }
    }

    fun openRoutinePreview(id: Long) {
        _previewRoutineId.update { id }
        _uiState.update { it.copy(routinePreviewOpen = true) }
    }

    fun closeRoutinePreview() {
        _uiState.update { it.copy(routinePreviewOpen = false) }
    }

    fun selectRoutine(id: Long?) {
        _selectedRoutineId.update { id }
        _editorState.update { it.copy(routineId = id) }
        closeRoutineSelector()
    }

    fun validateData(taskType: TaskType, taskData: String) : Boolean {
        when (taskType) {
            TaskType.NONE -> {return true}
            TaskType.TIMER -> {return true}
            TaskType.COUNTDOWN -> {
                if (taskData.isBlank()) return false
                if (taskData.toIntOrNull()?.let { it > 0 } != true) return false
                return true
            }
            TaskType.TEXT -> {
                if (taskData.isNotBlank()) {
                    return true
                }
            }
            //TODO validate math task
            TaskType.MATH -> {
                val s = taskData.split("|")
                if (s.size != 2) return false
                if (s[1].toIntOrNull()?.let { it > 0 } != true) return false
                try {
                    MathType.valueOf(s[0])
                } catch (e: Exception) {
                    return false
                }
                return true
            }
            TaskType.TARGET -> {
                if (taskData.isBlank()) return false
                if (taskData.toIntOrNull()?.let { it > 0 } == true) {
                    return true
                }
            }
            //TODO fix here and make real
            TaskType.GAME -> {
                return true
            }
            TaskType.BARCODE -> {
                return taskData.isNotBlank()
            }
            else -> {return false}
        }
        return false
    }

}

//TODO add remember for scanned codes

// UI observes `uiState` and reacts
data class UiState(
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val inputErrorMessage: String? = null,
    val routineSelectorOpen: Boolean = false,
    val routinePreviewOpen: Boolean = false,
)

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
    val snoozeCount: Int = 1,
    val routineId: Long? = null,
    val espEnabled: Boolean = true
) {
    enum class Mode { CREATE, EDIT }
}

fun trimTaskData(taskType: TaskType, taskData: String) : String {
    when (taskType) {
        TaskType.TEXT -> {
            return taskData.trim()
        }
        else -> {}
    }
    return taskData
}

fun parseData(taskType: TaskType, taskData: String) : String?  {

    when (taskType) {
        TaskType.NONE -> {return null}
        TaskType.TIMER -> {return null}
        TaskType.COUNTDOWN -> { return taskData }
        TaskType.TEXT -> {
            if (taskData.isNotBlank()) { return taskData }
            return ""
        }
        TaskType.MATH -> {return taskData}
        TaskType.TARGET -> { return taskData }
        TaskType.GAME -> {return taskData}
        TaskType.BARCODE -> {return taskData}
        TaskType.TASK_CHAIN -> {return null}
    }
}
//TODO remove else and add unimplemented to the






