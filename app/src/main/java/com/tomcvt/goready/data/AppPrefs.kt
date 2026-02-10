package com.tomcvt.goready.data

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStoreAppPrefs by preferencesDataStore(
    name = "app_prefs"
)

object AppPrefsKeys {
    val STEP_SEED_VERSION = intPreferencesKey("step_seed_version")
}