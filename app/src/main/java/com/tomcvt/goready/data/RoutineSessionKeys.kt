package com.tomcvt.goready.data

import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey


object RoutineSessionKeys {
    val ROUTINE_ID = stringPreferencesKey("routine_id")
    val STEP_ID = stringPreferencesKey("step_id")
    val STEP_STATUS = stringPreferencesKey("step_status") // "RUNNING", "PAUSED", "COMPLETED"
    val STEP_ENDS_AT = longPreferencesKey("step_ends_at")  // epoch millis
    val UI_SESSION_TOKEN = stringPreferencesKey("ui_session_token") // UUID string
    val LAST_UI_HEARTBEAT = longPreferencesKey("last_ui_heartbeat") // epoch millis
}