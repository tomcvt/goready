package com.tomcvt.goready.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val hour : Int,
    val minute : Int,
    val isEnabled : Boolean,
    val label : String,
    val task: String,
    val taskData: String,
    val repeatDays: Set<DayOfWeek>,
    val soundUri: String,
    val snoozeEnabled: Boolean,
    val snoozeDurationMinutes: Int,
    val snoozeMaxCount: Int
)