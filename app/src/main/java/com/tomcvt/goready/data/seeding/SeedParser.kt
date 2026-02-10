package com.tomcvt.goready.data.seeding

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json

class SeedParser(
    private val context: Context
) {
    fun parseSteps(fileName: String): StepSeedFile? {

        val assetManager = context.assets
        val inputStream = assetManager.open("seeds/steps/$fileName")
        val version = fileName.split("_")[0].substring(1).toInt()
        val flag = fileName.split("_")[1]
        if (flag !in listOf("N", "R") || version < 1) {
            Log.e("SeedParser", "Invalid file name: $fileName")
            return null
        }
        var replace = false
        if (flag == "R") {
            replace = true
        }
        val json = inputStream.bufferedReader().use { it.readText() }
        if (json.isEmpty()) {
            Log.e("SeedParser", "JSON file is empty: $fileName")
            return null
        }
        val stepsJsonTemplate = Json.decodeFromString<StepsJsonTemplate>(json)
        val stepSeedFile = StepSeedFile(
            version = version,
            replace = replace,
            stepDefinitions = stepsJsonTemplate.steps
        )
        return stepSeedFile
    }
}