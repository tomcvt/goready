@file:OptIn(ExperimentalCoroutinesApi::class)

package com.tomcvt.goready.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomcvt.goready.constants.StepType
import com.tomcvt.goready.constants.StepTypeSelector
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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
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

    private val _stepTypeSelector = MutableStateFlow<StepTypeSelector>(StepTypeSelector.Add)
    val stepTypeSelector: StateFlow<StepTypeSelector> = _stepTypeSelector
    val selectedStepType: StateFlow<StepType?> =
        stepTypeSelector
            .map {
                if (it is StepTypeSelector.SelectedType) it.type else null
            }
            .stateIn(
                viewModelScope,
                started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(1000),
                initialValue = null
            )

    val userStepDefs: StateFlow<List<StepDefinitionEntity>> =
        routinesManager.getUserStepDefinitionsFlow().stateIn(
            viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(1000),
            initialValue = emptyList()
        )

    val selectedStepsByType: StateFlow<List<StepDefinitionEntity>> =
        stepTypeSelector
            .mapNotNull {
                if (it is StepTypeSelector.SelectedType) it.type else null
            }
            .flatMapLatest { type ->
                routinesManager.getStepDefinitionsByTypeFlow(type)
            }.stateIn(
                viewModelScope,
                started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(1000),
                initialValue = emptyList()
            )

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
                    null,
                    it.stepType,
                    it.name,
                    it.description,
                    it.icon,
                    it.updatable
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

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
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
        _stepEditorState.value = StepDefinitionState()
        _uiState.update { it.copy(isStepEditorOpen = true) }
    }

    fun setStepTypeSelector(type: StepTypeSelector) {
        _stepTypeSelector.value = type
    }

    fun openStepEditorWithStep(step: StepDefinitionEntity, index: Int) {
        _stepEditorState.update {
            it.copy(
                index = index,
                id = step.id,
                stepType = step.stepType,
                name = step.name,
                description = step.description,
                icon = step.icon
            )
        }
        _uiState.update { it.copy(isStepEditorOpen = true) }
    }

    fun closeStepEditor() {
        _uiState.update { it.copy(isStepEditorOpen = false) }
        _stepTypeSelector.value = StepTypeSelector.Add
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

    fun setStepType(type: StepType) {
        _stepEditorState.update { it.copy(stepType = type) }
    }

    fun setStepModalNumber(number: Int) {
        _uiState.update { it.copy(stepModalNumber = number) }
    }

    fun clearStepModalNumber() {
        _uiState.update { it.copy(stepModalNumber = null) }
    }

    fun saveStepDefinition() {
        if (_stepEditorState.value.id == 0L) {
            saveStepDefinitionAndAdd()
        } else {
            updateStepDefinitionAndSet()
        }
    }

    //TODO keep editing step id, init step editor with selected step, save on edit

    fun addStepDefToRoutineEditor(step: StepDefinitionEntity) {
        val index = _routineEditorState.value.steps.size
        addStepDefToRoutineEditor(step, index)
        cleanStepEditor()
        closeStepEditor()
        closeStepAdder()
        _uiState.update { it.copy(successMessage = "Step definition added") }
    }

    fun saveStepDefinitionAndAdd() {
        viewModelScope.launch {
            val s = stepEditorState.value
            val errorMsg = validateStepData(s)
            if (errorMsg != null) {
                _uiState.update { it.copy(errorMessage = errorMsg) }
                return@launch
            }
            val draft = StepDefinitionDraft(
                id = s.id,
                stepType = s.stepType?: StepType.OTHER, //checked in validation,
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
            //closeStepEditor()
            closeStepAdder()
            _uiState.update { it.copy(successMessage = "Step definition added") }
            Log.d("RoutinesViewModel", "Step definition added")
        }
    }

    //TODO if about update or save for step editor

    fun updateStepDefinitionAndSet() {
        viewModelScope.launch {
            val s = stepEditorState.value
            val index = s.index?: return@launch
            //TODO val errormsg = validate Step (if type chosen)
            val errorMsg = validateStepData(s)
            if (errorMsg != null) {
                _uiState.update { it.copy(errorMessage = errorMsg) }
                return@launch
            }
            val draft = StepDefinitionDraft(
                id = s.id,
                stepType = s.stepType?: StepType.OTHER, //checked in validation
                name = s.name,
                description = s.description,
                icon = s.icon
            )
            routinesManager.updateStepDefinition(draft)
            val stepDefinition = routinesManager.getStepDefinition(s.id)
            if (stepDefinition != null) {
                replaceStepDefInRoutineEditor(stepDefinition, index)
            }
            cleanStepEditor()
            closeStepEditor()
            _uiState.update { it.copy(successMessage = "Step definition updated") }
        }
    }

    private fun replaceStepDefInRoutineEditor(step: StepDefinitionEntity, position: Int) {
        val currentSteps = _routineEditorState.value.steps.toMutableList()
        currentSteps[position] = Pair(step, 15)
        _routineEditorState.value = _routineEditorState.value.copy(steps = currentSteps)
    }

    private fun addStepDefToRoutineEditor(step: StepDefinitionEntity, position: Int) {
        val currentSteps = _routineEditorState.value.steps.toMutableList()
        currentSteps.add(position, Pair(step, 15))
        _routineEditorState.value = _routineEditorState.value.copy(steps = currentSteps)
    }

    fun removeStepFromRoutineEditor(position: Int) {
        val currentSteps = _routineEditorState.value.steps.toMutableList()
        currentSteps.removeAt(position)
        _routineEditorState.value = _routineEditorState.value.copy(steps = currentSteps)
    }

    fun cleanStepEditor() {
        _stepEditorState.value = StepDefinitionState()
    }

    fun openStepAdder() {
        _uiState.update { it.copy(isStepAdderOpen = true) }
    }

    fun closeStepAdder() {
        _uiState.update { it.copy(isStepAdderOpen = false) }
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
            val errorMsg = validateRoutineData(s)
            if (errorMsg != null) {
                _uiState.update { it.copy(errorMessage = errorMsg) }
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
            closeRoutineEditor()
            clearRoutineEditor()
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
    val index: Int? = null,
    val id: Long = 0,
    val stepType: StepType? = null,
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
    val isStepAdderOpen: Boolean = false,
    val isRoutineDetailsOpen: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

sealed class UiEvent {
    data class OpenRoutineLauncher(val routineId: Long) : UiEvent()
}

private fun validateStepData(state: StepDefinitionState) : String? {
    //TODO for now we dont care about the type
    if (state.stepType == StepType.NONE || state.stepType == null) return "Select step type"
    if (state.name.isBlank()) return "Name is empty"
    return null
}

private fun validateRoutineData(state: RoutineEditorState) : String? {
    if (state.name.isBlank()) return "Name is empty"
    if (state.steps.isEmpty()) return "No steps are added"
    for (step in state.steps) {
        if (step.first == null) return "Error, reload editor"
        if (step.second <= 0) return "Time cant be less or equal to zero"
    }
    return null
}
