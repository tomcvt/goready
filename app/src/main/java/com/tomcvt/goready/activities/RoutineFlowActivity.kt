package com.tomcvt.goready.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.tomcvt.goready.application.AlarmApp
import com.tomcvt.goready.manager.AppRoutinesManager
import com.tomcvt.goready.repository.RoutineRepository
import com.tomcvt.goready.repository.RoutineStepRepository
import com.tomcvt.goready.repository.StepDefinitionRepository
import com.tomcvt.goready.viewmodel.RoutinesViewModelFactory


private const val TAG = "RoutineFlowActivity"
class RoutineFlowActivity : ComponentActivity() {

    lateinit var appObject: AlarmApp
        private set

    lateinit var appRoutinesManager: AppRoutinesManager
        private set

    lateinit var routinesViewModelFactory: RoutinesViewModelFactory
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appObject = (application as AlarmApp)
        appRoutinesManager = AppRoutinesManager(
            RoutineRepository(appObject.db.routineDao()),
            RoutineStepRepository(appObject.db.routineStepDao()),
            StepDefinitionRepository(appObject.db.stepDefinitionDao())
        )
        routinesViewModelFactory = RoutinesViewModelFactory(appRoutinesManager)

        val routineId = intent.getLongExtra("routineId", -1)
        val routineName = intent.getStringExtra("routineName")


    }
}