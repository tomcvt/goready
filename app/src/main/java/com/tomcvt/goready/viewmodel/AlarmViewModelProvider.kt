package com.tomcvt.goready.viewmodel

import androidx.activity.ComponentActivity
import com.tomcvt.goready.data.AlarmDatabase
import com.tomcvt.goready.manager.AppAlarmManager
import com.tomcvt.goready.manager.SystemAlarmScheduler
import com.tomcvt.goready.repository.AlarmRepository

object AlarmViewModelProvider {

    private var instance: AlarmViewModel? = null

    fun provideAlarmViewModel(activity: ComponentActivity): AlarmViewModel {
        if (instance == null) {
            // Create dependencies manually (manager, repository)
            val db = AlarmDatabase.getDatabase(activity) // your Room singleton
            val repository = AlarmRepository(db.alarmDao())
            val manager = AppAlarmManager(repository, SystemAlarmScheduler(activity))

            instance = AlarmViewModel(manager)
        }
        return instance!!
    }
}

