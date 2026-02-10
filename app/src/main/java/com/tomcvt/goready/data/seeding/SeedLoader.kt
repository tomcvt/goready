package com.tomcvt.goready.data.seeding

import android.content.Context

class SeedLoader(
    private val context: Context
) {
    fun loadStepSeeds(): List<StepSeedFile> {
        val assetManager = context.assets
        val seedFiles = assetManager.list("seeds/steps") ?: emptyArray()

        return seedFiles
            .mapNotNull { fileName ->
                SeedParser(context).parseSteps(fileName)
            }
    }
}