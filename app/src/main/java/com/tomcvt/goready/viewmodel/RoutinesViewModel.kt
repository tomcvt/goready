package com.tomcvt.goready.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomcvt.goready.domain.StepDefinitionDraft
import com.tomcvt.goready.manager.AppRoutinesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
            routinesManager.addStepDefinition(draft)
        }

    }


}

data class StepDefinitionState (
    val stepType: String = "",
    val name: String = "",
    val description: String = "",
    val icon: String = ""
)

private fun validateStepData(state: StepDefinitionState) : Boolean {
    if (state.stepType.isBlank()) return false
    if (state.name.isBlank()) return false
    if (state.description.isBlank()) return false
    return true
}
