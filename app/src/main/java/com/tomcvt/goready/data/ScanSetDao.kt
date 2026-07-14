package com.tomcvt.goready.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanSetDao {
    @Query("SELECT * FROM scan_sets")
    fun getScanSets(): Flow<List<ScanSetEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanSet(scanSet: ScanSetEntity) : Long
    @Delete
    suspend fun deleteScanSet(scanSet: ScanSetEntity)
    @Update
    suspend fun updateScanSet(scanSet: ScanSetEntity)
    @Query("SELECT * FROM scan_sets WHERE id = :id")
    suspend fun getScanSetById(id: Long): ScanSetEntity
}