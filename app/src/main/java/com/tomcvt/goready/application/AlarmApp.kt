package com.tomcvt.goready.application

import android.app.Application
import android.util.Log
import androidx.emoji2.text.EmojiCompat
import com.tomcvt.goready.BuildConfig
import com.tomcvt.goready.data.AlarmDatabase
import com.tomcvt.goready.data.seeding.SeedManager
import com.tomcvt.goready.manager.AppAlarmManager
import com.tomcvt.goready.manager.RoutineFlowManager
import com.tomcvt.goready.manager.RoutineScheduler
import com.tomcvt.goready.manager.SystemAlarmScheduler
import com.tomcvt.goready.premium.DevPremiumRepository
import com.tomcvt.goready.premium.PremiumRepositoryI
import com.tomcvt.goready.premium.ProdPremiumRepository
import com.tomcvt.goready.repository.AlarmRepositoryImpl
import com.tomcvt.goready.repository.AppPrefsRepository
import com.tomcvt.goready.repository.RoutineRepository
import com.tomcvt.goready.repository.RoutineSessionRepository
import com.tomcvt.goready.repository.RoutineStepRepository
import com.tomcvt.goready.repository.StepDefinitionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlarmApp : Application() {
    lateinit var alarmRepository: AlarmRepositoryImpl
        private set
    lateinit var routineRepository: RoutineRepository
        private set
    lateinit var routineStepRepository: RoutineStepRepository
        private set
    lateinit var stepDefinitionRepository: StepDefinitionRepository
        private set
    lateinit var routineSessionRepository: RoutineSessionRepository
        private set
    lateinit var premiumRepository: PremiumRepositoryI
        private set
    lateinit var db : AlarmDatabase
        private set
    lateinit var systemAlarmScheduler: SystemAlarmScheduler
        private set
    lateinit var appAlarmManager: AppAlarmManager
        private set
    lateinit var routineFlowManager: RoutineFlowManager
        private set
    lateinit var routineScheduler: RoutineScheduler
        private set

    override fun onCreate() {
        super.onCreate()

        val appContext = applicationContext

        EmojiCompat.init(this)

        db = AlarmDatabase.getDatabase(this)

        val appPrefsRepository = AppPrefsRepository(this)


        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            SeedManager(
                appPrefsRepository = appPrefsRepository,
                database = db,
                context = this@AlarmApp
            ).applyStepsSeeds()
            Log.d("SeedManager", "Steps seeds applied")
        }


        alarmRepository = AlarmRepositoryImpl(db.alarmDao())

        systemAlarmScheduler = SystemAlarmScheduler(this)

        appAlarmManager = AppAlarmManager(alarmRepository, systemAlarmScheduler)


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

        premiumRepository =
            if (BuildConfig.DEBUG) {
                DevPremiumRepository()
            } else {
                ProdPremiumRepository()
            }

    }
}