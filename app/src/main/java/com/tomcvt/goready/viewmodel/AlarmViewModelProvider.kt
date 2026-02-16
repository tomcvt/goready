package com.tomcvt.goready.viewmodel

import androidx.activity.ComponentActivity
import com.tomcvt.goready.data.AlarmDatabase
import com.tomcvt.goready.manager.AppAlarmManagerImpl
import com.tomcvt.goready.manager.SystemAlarmScheduler
import com.tomcvt.goready.repository.AlarmRepositoryImpl

object AlarmViewModelProvider {

    private var instance: AlarmViewModel? = null

    fun provideAlarmViewModel(activity: ComponentActivity): AlarmViewModel {
        if (instance == null) {
            // Create dependencies manually (manager, repository)
            val db = AlarmDatabase.getDatabase(activity) // your Room singleton
            val repository = AlarmRepositoryImpl(db.alarmDao())
            val manager = AppAlarmManagerImpl(repository, SystemAlarmScheduler(activity))

            //instance = AlarmViewModel(manager)
        }
        return instance!!
    }
}

