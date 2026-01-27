package com.tomcvt.goready.application

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.emoji2.text.EmojiCompat
import com.tomcvt.goready.data.AlarmDatabase
import com.tomcvt.goready.repository.AlarmRepository

class AlarmApp : Application() {
    lateinit var alarmRepository: AlarmRepository
        private set

    lateinit var db : AlarmDatabase
        private set

    val Context.routineDataStore by preferencesDataStore(name = "routine_session")

    override fun onCreate() {
        super.onCreate()
        db = AlarmDatabase.getDatabase(this)
        alarmRepository = AlarmRepository(db.alarmDao())
        //TODO init alarm manager in activities, check if works
        EmojiCompat.init(this)

    }
}