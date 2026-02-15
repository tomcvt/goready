package com.tomcvt.goready.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hour : Int = 7,
    val minute : Int = 0,
    val isEnabled : Boolean = true,
    val label : String? = null,
    val task: String? = null,
    val taskData: String? = null,
    val repeatDays: Set<DayOfWeek> = emptySet(),
    val soundUri: String? = null,
    val snoozeEnabled: Boolean = false,
    val snoozeDurationMinutes: Int? = 0,
    val snoozeMaxCount: Int? = 0,
    val routineId: Long? = null,
    val systemHookId: Int? = null
)