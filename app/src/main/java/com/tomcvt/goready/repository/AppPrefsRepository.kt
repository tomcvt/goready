package com.tomcvt.goready.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.tomcvt.goready.data.AppPrefsKeys.STEP_SEED_VERSION
import com.tomcvt.goready.data.dataStoreAppPrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.collections.get

class AppPrefsRepository(
    private val context: Context
) {
    val stepsSeedVersionFlow: Flow<Int> =
        context.dataStoreAppPrefs.data.map{ prefs ->
            prefs[STEP_SEED_VERSION] ?: 0
        }

    suspend fun getSeedVersion(): Int {
        return context.dataStoreAppPrefs.data.map {
            it[STEP_SEED_VERSION] ?: 0
        }.first()
    }

    suspend fun setSeedVersion(version: Int) {
        context.dataStoreAppPrefs.edit { prefs ->
            prefs[STEP_SEED_VERSION] = version
        }
    }
}