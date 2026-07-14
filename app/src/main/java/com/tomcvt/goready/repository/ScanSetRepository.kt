package com.tomcvt.goready.repository

import com.tomcvt.goready.data.ScanSetEntity
import com.tomcvt.goready.scanner.ScanCode
import kotlinx.coroutines.flow.Flow

interface ScanSetRepository {
    fun getScanSets(): Flow<List<ScanSetEntity>>
    suspend fun insertScanSet(scanSet: ScanSetEntity) : Long
    suspend fun deleteScanSet(scanSet: ScanSetEntity)
    suspend fun updateScanSet(scanSet: ScanSetEntity)
    suspend fun getScanSetById(id: Long): ScanSetEntity
    fun decodeScanCodes(encodedCodes: String): List<ScanCode>
    fun encodeScanCodes(codes: List<ScanCode>): String
}