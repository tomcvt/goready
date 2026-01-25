package com.tomcvt.goready.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomcvt.goready.data.StepDefinitionEntity
import com.tomcvt.goready.data.StepWithDefinition
import com.tomcvt.goready.domain.RoutineDraft
import com.tomcvt.goready.domain.StepDefinitionDraft
import com.tomcvt.goready.manager.AppRoutinesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RoutinesViewModel(
    private val routinesManager: AppRoutinesManager
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    private val _stepEditorState =
        MutableStateFlow(StepDefinitionState())
    val stepEditorState: StateFlow<StepDefinitionState> =
        _stepEditorState.asStateFlow()

    private val _routineEditorState = MutableStateFlow<RoutineState>(RoutineState())

    val routineEditorState: StateFlow<RoutineState> = _routineEditorState.asStateFlow()


    fun saveStepDefinition() {
        viewModelScope.launch {
            val s = stepEditorState.value
            if(!validateStepData(s)) {
                _uiState.value = UiState.InputError("Provide all data")
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
            _uiState.value = UiState.Success("Step definition added")
        }
    }

    fun saveStepDefinitionAndAdd() {
        viewModelScope.launch {
            val s = stepEditorState.value
            if(!validateStepData(s)) {
                _uiState.value = UiState.InputError("Provide all data")
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
            _stepEditorState.value = StepDefinitionState(lastAddedId = addedId)
            _uiState.value = UiState.Success("Step definition added")
        }
    }

    private fun addStepDefToRoutineEditor(step: StepDefinitionEntity, position: Int) {
        val currentSteps = _routineEditorState.value.steps.toMutableList()
        currentSteps.add(position, Pair(step, 15))
        _routineEditorState.value = _routineEditorState.value.copy(steps = currentSteps)
    }

    fun startNew() {
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
                _uiState.value = UiState.InputError("Provide all data")
            }
            val draft = RoutineDraft(
                name = s.name,
                description = s.description,
                icon = s.icon,
                steps = s.steps
            )
            routinesManager.addRoutine(draft)
            _uiState.value = UiState.Success("Routine added")
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

data class RoutineState(
    val name: String = "",
    val description: String = "",
    val icon: String = "",
    val steps: List<Pair<StepDefinitionEntity,Int>> = emptyList()
)

private fun validateStepData(state: StepDefinitionState) : Boolean {
    if (state.stepType.isBlank()) return false
    if (state.name.isBlank()) return false
    return true
}

private fun validateRoutineData(state: RoutineState) : Boolean {
    if (state.name.isBlank()) return false
    if (state.steps.isEmpty()) return false
    for (step in state.steps) {
        if (step.first == null) return false
        if (step.second <= 0) return false
    }
    return true
}
