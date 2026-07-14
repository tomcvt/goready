package com.tomcvt.goready.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_sets")
data class ScanSetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val encodedCodes: String,
    val size: Int
)