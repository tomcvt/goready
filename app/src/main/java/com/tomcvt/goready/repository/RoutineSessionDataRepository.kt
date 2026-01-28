package com.tomcvt.goready.repository;


import android.content.Context
import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.tomcvt.goready.data.RoutineSessionKeys
import com.tomcvt.goready.data.StepStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

val Context.routineDataStore by preferencesDataStore(name = "routine_session")

class RoutineSessionDataRepository(
    private val dataStore: DataStore<Preferences>
) {

    val sessionFlow: Flow<RoutineSessionData> = dataStore.data.map { prefs ->
        RoutineSessionData(
            routineId = prefs[RoutineSessionKeys.ROUTINE_ID],
            stepId = prefs[RoutineSessionKeys.STEP_ID],
            stepStatus = prefs[RoutineSessionKeys.STEP_STATUS]?.let { StepStatus.valueOf(it) },
            stepEndsAt = prefs[RoutineSessionKeys.STEP_ENDS_AT],
            uiSessionToken = prefs[RoutineSessionKeys.UI_SESSION_TOKEN],
            lastUiHeartbeatAt = prefs[RoutineSessionKeys.LAST_UI_HEARTBEAT]
        )
    }

    suspend fun startStep(routineId: String, stepId: String, stepEndsAt: Long) {
        dataStore.edit { prefs ->
            prefs[RoutineSessionKeys.ROUTINE_ID] = routineId
            prefs[RoutineSessionKeys.STEP_ID] = stepId
            prefs[RoutineSessionKeys.STEP_STATUS] = StepStatus.RUNNING.name
            prefs[RoutineSessionKeys.STEP_ENDS_AT] = stepEndsAt
            prefs[RoutineSessionKeys.UI_SESSION_TOKEN] = UUID.randomUUID().toString()
            prefs[RoutineSessionKeys.LAST_UI_HEARTBEAT] = System.currentTimeMillis()
        }
    }

    suspend fun pauseStep() {
        dataStore.edit { prefs ->
            prefs[RoutineSessionKeys.STEP_STATUS] = StepStatus.PAUSED.name
        }
    }

    suspend fun completeStep() {
        dataStore.edit { prefs ->
            prefs[RoutineSessionKeys.STEP_STATUS] = StepStatus.COMPLETED.name
        }
    }

    suspend fun heartbeat() {
        dataStore.edit { prefs ->
            prefs[RoutineSessionKeys.LAST_UI_HEARTBEAT] = System.currentTimeMillis()
        }
    }

    suspend fun clearSession() {
        dataStore.edit { it.clear() }
    }
}

data class RoutineSessionData(
    val routineId: String? = null,
    val stepId: String? = null,
    val stepStatus: StepStatus? = null,
    val stepEndsAt: Long? = null,
    val uiSessionToken: String? = null,
    val lastUiHeartbeatAt: Long? = null
)
