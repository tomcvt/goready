package com.tomcvt.goready.application

import android.app.Application
import com.tomcvt.goready.data.AlarmDatabase
import com.tomcvt.goready.manager.AlarmManager
import com.tomcvt.goready.manager.SystemAlarmScheduler
import com.tomcvt.goready.repository.AlarmRepository

class AlarmApp : Application() {

    lateinit var alarmManager: AlarmManager
        private set

    override fun onCreate() {
        super.onCreate()

        val db = AlarmDatabase.getDatabase(this)
        val repository = AlarmRepository(db.alarmDao())
        alarmManager = AlarmManager(repository, SystemAlarmScheduler(this))
    }
}