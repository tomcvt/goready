package com.tomcvt.goready.application

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.emoji2.text.EmojiCompat
import com.tomcvt.goready.data.AlarmDatabase
import com.tomcvt.goready.manager.RoutineFlowManager
import com.tomcvt.goready.manager.RoutineScheduler
import com.tomcvt.goready.repository.AlarmRepository
import com.tomcvt.goready.repository.RoutineRepository
import com.tomcvt.goready.repository.RoutineSessionRepository
import com.tomcvt.goready.repository.RoutineStepRepository
import com.tomcvt.goready.repository.StepDefinitionRepository

class AlarmApp : Application() {
    lateinit var alarmRepository: AlarmRepository
        private set

    lateinit var routineRepository: RoutineRepository
        private set

    lateinit var routineStepRepository: RoutineStepRepository
        private set

    lateinit var stepDefinitionRepository: StepDefinitionRepository
        private set

    lateinit var routineSessionRepository: RoutineSessionRepository
        private set

    lateinit var db : AlarmDatabase
        private set

    lateinit var routineFlowManager: RoutineFlowManager
        private set

    lateinit var routineScheduler: RoutineScheduler
        private set

    override fun onCreate() {
        super.onCreate()

        val appContext = applicationContext

        db = AlarmDatabase.getDatabase(this)
        alarmRepository = AlarmRepository(db.alarmDao())
        //TODO init alarm manager in activities, check if works
        EmojiCompat.init(this)

        routineRepository = RoutineRepository(db.routineDao())
        routineStepRepository = RoutineStepRepository(db.routineStepDao())
        stepDefinitionRepository = StepDefinitionRepository(db.stepDefinitionDao())
        routineSessionRepository = RoutineSessionRepository(db.routineSessionDao())

        routineScheduler = RoutineScheduler(this)

        routineFlowManager = RoutineFlowManager(
            routineRepository,
            routineStepRepository,
            stepDefinitionRepository,
            routineSessionRepository,
            routineScheduler,
            appContext
        )

    }
}