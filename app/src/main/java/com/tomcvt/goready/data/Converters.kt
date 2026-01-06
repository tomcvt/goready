package com.tomcvt.goready.data

import androidx.room.TypeConverter
import java.time.DayOfWeek

class Converters {
    @TypeConverter
    fun fromDays(days: Set<DayOfWeek>): String =
        days.joinToString(",") { it.name }  // String → SQLite TEXT column

    @TypeConverter
    fun toDays(data: String): Set<DayOfWeek> =
        if (data.isEmpty()) emptySet() else data.split(",").map { DayOfWeek.valueOf(it) }.toSet()
}