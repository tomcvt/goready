package com.tomcvt.goready.data

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val icon: String,
)
