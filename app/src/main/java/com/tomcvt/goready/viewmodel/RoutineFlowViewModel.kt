@file:OptIn(ExperimentalCoroutinesApi::class)

package com.tomcvt.goready.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomcvt.goready.data.RoutineEntity
import com.tomcvt.goready.data.RoutineSession
import com.tomcvt.goready.data.RoutineStatus
import com.tomcvt.goready.data.StepStatus
import com.tomcvt.goready.data.StepWithDefinition
import com.tomcvt.goready.manager.RoutineFlowManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MINUTE = 60000L

class RoutineFlowViewModel(
    private val routineFlowManager: RoutineFlowManager
) : ViewModel( ) {

    private val _flowUiState = MutableStateFlow<FlowUiState>(FlowUiState())
    val flowUiState: StateFlow<FlowUiState> = _flowUiState
    val selectedSessionId = MutableStateFlow<Long?>(null)

    fun selectSession(sessionId: Long) {
        selectedSessionId.value = sessionId
    }

    fun launchRoutine(routineId: Long) {
        viewModelScope.launch {
            val sessionId = routineFlowManager.startRoutine(routineId)
            selectedSessionId.value = sessionId
        }
    }

    val sessionState: StateFlow<RoutineSession?> =
        selectedSessionId
            .filterNotNull()
            .flatMapLatest { id ->
                routineFlowManager.getRoutineSessionByIdFlow(id)
            }
            .filterNotNull()
            .stateIn(
                viewModelScope,
                started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(1000),
                initialValue = null
            )
    val launcherRoutine: StateFlow<RoutineEntity?> =
        flowUiState
            .flatMapLatest {
                routineFlowManager.getRoutineByIdFlow(it.launcherRoutineId?: 0)
            }
            .filterNotNull()
            .stateIn(
                viewModelScope,
                started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(1000),
                initialValue = null
            )

    val launcherRoutineSteps: StateFlow<List<StepWithDefinition>> =
        flowUiState
            .flatMapLatest {
                routineFlowManager.getRoutineStepsWithDefinitionFlow(it.launcherRoutineId?: 0)
            }
            .filterNotNull()
            .stateIn(
                viewModelScope,
                started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(1000),
                initialValue = emptyList()
            )

    val currentRoutineSteps: StateFlow<List<StepWithDefinition>> =
        sessionState
            .filterNotNull()
            .flatMapLatest {
                routineFlowManager.getRoutineStepsWithDefinitionFlow(it.routineId)
            }
            .stateIn(
                viewModelScope,
                started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(1000),
                initialValue = emptyList()
            )

    val currentRoutine: StateFlow<RoutineEntity?> =
        sessionState
            .filterNotNull()
            .flatMapLatest { session ->
                routineFlowManager.getRoutineByIdFlow(session.routineId)
            }
            .stateIn(
                viewModelScope,
                started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(1000),
                initialValue = null
            )

    val currentStep: StateFlow<StepWithDefinition?> =
        sessionState
            .filterNotNull()
            .flatMapLatest { session ->
                routineFlowManager.getRoutineStepByNumberFlow(session.routineId, session.stepNumber)
            }
            .stateIn(
                viewModelScope,
                started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(1000),
                initialValue = null
            )

    val currentStepFinishTime: StateFlow<Long?> =
        combine(currentStep, sessionState) { step, session ->
            if (step == null || session == null) return@combine null

            step.length * MINUTE + session.stepStartTime
        }
        .stateIn(
            viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(1000),
            initialValue = null
        )

    fun closeRoutineLauncher() {
        _flowUiState.update { it.copy(launcherOverlay = false, launcherRoutineId = null) }
    }

    fun setLauncherRoutine(routineId: Long) {
        _flowUiState.update { it.copy(launcherRoutineId = routineId) }
    }

    fun setLauncherOverlay(show: Boolean) {
        _flowUiState.update { it.copy(launcherOverlay = show) }
    }

    fun nextStep() {
        viewModelScope.launch {
            routineFlowManager.advanceToNextStep(selectedSessionId.value)
        }
    }
}

data class FlowUiState(
    val launcherOverlay: Boolean = false,
    val launcherRoutineId: Long? = null
)