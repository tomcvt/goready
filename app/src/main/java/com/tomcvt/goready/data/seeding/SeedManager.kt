package com.tomcvt.goready.data.seeding

import android.content.Context
import android.util.Log
import com.tomcvt.goready.data.AlarmDatabase
import com.tomcvt.goready.repository.AppPrefsRepository

class SeedManager(
    private val appPrefsRepository: AppPrefsRepository,
    private val database: AlarmDatabase,
    private val context: Context
) {

    suspend fun applyStepsSeeds() {
        val currentVersion = appPrefsRepository.getSeedVersion()
        val seeds = SeedLoader(context).loadStepSeeds()

        seeds
            .filter { it.version > currentVersion }
            .sortedBy { it.version }
            .forEach { seed ->
                applyStepSeed(seed)
                appPrefsRepository.setSeedVersion(seed.version)
            }
    }

    private suspend fun applyStepSeed(seed: StepSeedFile) {
        if (seed.replace) {
            Log.d("SeedManager", "Replacing step definitions: ${seed.stepDefinitions.size}")
            database.stepDefinitionDao().insertSeedStepDefinitionsReplace(seed.stepDefinitions)
        } else {
            Log.d("SeedManager", "Inserting step definitions: ${seed.stepDefinitions.size}")
            database.stepDefinitionDao().insertSeedStepDefinitions(seed.stepDefinitions)
        }
    }
}