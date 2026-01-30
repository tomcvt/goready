@file:OptIn(ExperimentalCoroutinesApi::class)

package com.tomcvt.goready.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomcvt.goready.data.RoutineEntity
import com.tomcvt.goready.data.StepDefinitionEntity
import com.tomcvt.goready.data.StepWithDefinition
import com.tomcvt.goready.domain.RoutineDraft
import com.tomcvt.goready.domain.StepDefinitionDraft
import com.tomcvt.goready.manager.AppRoutinesManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "RoutinesViewModel"

class RoutinesViewModel(
    private val routinesManager: AppRoutinesManager
) : ViewModel() {
    //private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    //val uiState: StateFlow<UiState> = _uiState
    private val _uiState = MutableStateFlow<RoutineUiState>(RoutineUiState())
    val uiState: StateFlow<RoutineUiState> = _uiState.asStateFlow()
    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    fun openRoutineLauncher(routineId: Long) {
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.OpenRoutineLauncher(routineId))
        }
    }

    private val _stepEditorState =
        MutableStateFlow(StepDefinitionState())
    val stepEditorState: StateFlow<StepDefinitionState> =
        _stepEditorState.asStateFlow()

    private val _routineEditorState = MutableStateFlow<RoutineEditorState>(RoutineEditorState())
    val routineEditorState: StateFlow<RoutineEditorState> = _routineEditorState

    val routinesStateFlow: StateFlow<List<RoutineEntity>> = routinesManager
        .getAllRoutinesFlow().stateIn(
            viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(1000),
            initialValue = emptyList()
        )

    private val selectedRoutineId = MutableStateFlow<Long?>(null)

    val selectedRoutineSteps: StateFlow<List<StepWithDefinition>> =
        selectedRoutineId.filterNotNull()
            .flatMapLatest { id ->
                routinesManager.getRoutineStepsWithDefinitionFlow(id)
            }
            .stateIn(
                viewModelScope,
                started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(1000),
                initialValue = emptyList()
            )

    val selectedRoutineEntity: StateFlow<RoutineEntity?> =
        selectedRoutineId.filterNotNull()
            .flatMapLatest { id ->
                routinesManager.getRoutineByIdFlow(id)
            }.filterNotNull()
            .stateIn(
                viewModelScope,
                started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(1000),
                initialValue = null
            )

    fun openRoutineEditorWithSelectedRoutine() {
        viewModelScope.launch {
            val routineId = selectedRoutineId.value ?: return@launch
            val routine = routinesManager.getRoutineById(routineId) ?: return@launch
            val stepsFlow = routinesManager.getRoutineStepsWithDefinitionFlow(routineId)
            val steps = stepsFlow.first()

            //TODO think about emmbedable
            val stepsList = steps.map(
                transform = { Pair(StepDefinitionEntity(
                    it.stepId,
                    it.stepType,
                    it.name,
                    it.description,
                    it.icon
                ), it.length.toInt()) }
            )
            _routineEditorState.update {
                it.copy(
                    id = routine.id,
                    name = routine.name,
                    description = routine.description,
                    icon = routine.icon,
                    steps = stepsList
                )
            }
            _uiState.update { it.copy(isRoutineEditorOpen = true) }
        }
    }

    fun openRoutineDetails() {
        _uiState.update { it.copy(isRoutineDetailsOpen = true) }
    }

    fun closeRoutineDetails() {
        _uiState.update { it.copy(isRoutineDetailsOpen = false) }
    }

    //fun getStepWithDefinitionFlow(routineId: Long) = routinesManager.getRoutineStepsFlow(routineId)

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    //routineId not used here
    fun addRoutineInEditor(routineId: Long) {
        _routineEditorState.value = RoutineEditorState()
        _uiState.update { it.copy(isRoutineEditorOpen = true) }
    }

    fun setRoutineName(name: String) {
        _routineEditorState.update { it.copy(name = name) }
    }

    fun setRoutineDescription(description: String) {
        _routineEditorState.update { it.copy(description = description) }
    }
    fun setRoutineIcon(icon: String) {
        _routineEditorState.update { it.copy(icon = icon) }
    }

    fun closeRoutineEditor() {
        _uiState.update { it.copy(isRoutineEditorOpen = false) }
    }

    fun clearRoutineEditor() {
        _routineEditorState.value = RoutineEditorState()
    }

    fun selectRoutine(routineId: Long) {
        selectedRoutineId.value = routineId
    }

    fun unselectRoutine() {
        selectedRoutineId.value = null
    }

    fun openStepEditor() {
        _uiState.update { it.copy(isStepEditorOpen = true) }
    }

    fun closeStepEditor() {
        _uiState.update { it.copy(isStepEditorOpen = false) }
    }

    fun setStepName(name: String) {
        _stepEditorState.update { it.copy(name = name) }
    }

    fun setStepDescription(description: String) {
        _stepEditorState.update { it.copy(description = description) }
    }

    fun setStepIcon(icon: String) {
        _stepEditorState.update { it.copy(icon = icon) }
    }

    fun setStepModalNumber(number: Int) {
        _uiState.update { it.copy(stepModalNumber = number) }
    }

    fun clearStepModalNumber() {
        _uiState.update { it.copy(stepModalNumber = null) }
    }


    fun saveStepDefinition() {
        viewModelScope.launch {
            val s = stepEditorState.value
            if(!validateStepData(s)) {
                _uiState.update { it.copy(errorMessage = "Provide all data") }
                return@launch
            }
            val draft = StepDefinitionDraft(
                stepType = s.stepType,
                name = s.name,
                description = s.description,
                icon = s.icon
            )
            val addedId = routinesManager.addStepDefinition(draft)
            _stepEditorState.value = StepDefinitionState(lastAddedId = addedId)
            _uiState.update { it.copy(successMessage = "Step definition added") }
        }
    }

    fun saveStepDefinitionAndAdd() {
        viewModelScope.launch {
            val s = stepEditorState.value
            if(!validateStepData(s)) {
                Log.d(TAG, "Invalid data: $s")
                _uiState.update { it.copy(errorMessage = "Provide all data") }
                return@launch
            }
            val draft = StepDefinitionDraft(
                stepType = s.stepType,
                name = s.name,
                description = s.description,
                icon = s.icon
            )
            val addedId = routinesManager.addStepDefinition(draft)
            val stepDefinition = routinesManager.getStepDefinition(addedId)
            val index = _routineEditorState.value.steps.size
            if (stepDefinition != null) {
                addStepDefToRoutineEditor(stepDefinition, index)
            }
            cleanStepEditor()
            _stepEditorState.value = StepDefinitionState(lastAddedId = addedId)
            _uiState.update { it.copy(successMessage = "Step definition added") }
            Log.d("RoutinesViewModel", "Step definition added")
        }
    }

    private fun addStepDefToRoutineEditor(step: StepDefinitionEntity, position: Int) {
        val currentSteps = _routineEditorState.value.steps.toMutableList()
        currentSteps.add(position, Pair(step, 15))
        _routineEditorState.value = _routineEditorState.value.copy(steps = currentSteps)
    }

    fun cleanStepEditor() {
        _stepEditorState.value = StepDefinitionState()
    }

    fun updateTimeForStepInEditor(time: Int, position: Int) {
        _routineEditorState.update {
            val currentSteps = it.steps.toMutableList()
            currentSteps[position] = Pair(currentSteps[position].first, time)
            it.copy(steps = currentSteps)
        }
    }

    fun deleteStepFromEditor(position: Int) {
        _routineEditorState.update {
            val currentSteps = it.steps.toMutableList()
            currentSteps.removeAt(position)
            it.copy(steps = currentSteps)
        }
    }

    fun saveRoutine() {
        viewModelScope.launch {
            val s = routineEditorState.value
            if (!validateRoutineData(s)) {
                _uiState.update { it.copy(errorMessage = "Provide all data") }
                return@launch
            }
            val draft = RoutineDraft(
                id = s.id,
                name = s.name,
                description = s.description,
                icon = s.icon,
                steps = s.steps
            )
            routinesManager.addRoutine(draft)
            _uiState.update { it.copy(successMessage = "Routine added") }
        }
    }

    fun deleteRoutine(routine: RoutineEntity) {
        viewModelScope.launch {
            routinesManager.deleteRoutine(routine)
        }
    }
}

data class StepDefinitionState (
    val lastAddedId: Long = 0,
    val stepType: String = "",
    val name: String = "",
    val description: String = "",
    val icon: String = ""
)

data class RoutineEditorState(
    val id: Long? = null,
    val name: String = "",
    val description: String = "",
    val icon: String = "",
    val steps: List<Pair<StepDefinitionEntity,Int>> = emptyList()
)

data class RoutineUiState(
    val stepModalNumber: Int? = null,
    val isRoutineEditorOpen: Boolean = false,
    val isStepEditorOpen: Boolean = false,
    val isRoutineDetailsOpen: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

sealed class UiEvent {
    data class OpenRoutineLauncher(val routineId: Long) : UiEvent()
}

private fun validateStepData(state: StepDefinitionState) : Boolean {
    //TODO for now we dont care about the type
    if (state.stepType.isBlank()) return true
    if (state.name.isBlank()) return false
    return true
}

private fun validateRoutineData(state: RoutineEditorState) : Boolean {
    if (state.name.isBlank()) return false
    if (state.steps.isEmpty()) return false
    for (step in state.steps) {
        if (step.first == null) return false
        if (step.second <= 0) return false
    }
    return true
}
