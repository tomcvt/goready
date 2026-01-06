package com.tomcvt.goready.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters


@Database(entities = [AlarmEntity::class], version = 1)
@TypeConverters(Converters::class)
abstract class AlarmDatabase: RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
}