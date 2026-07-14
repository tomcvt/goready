package com.tomcvt.goready.repository

import com.tomcvt.goready.data.ScanSetDao
import com.tomcvt.goready.data.ScanSetEntity
import com.tomcvt.goready.scanner.ScanCode
import com.tomcvt.goready.scanner.decodeScanCodesR
import com.tomcvt.goready.scanner.encodeScanCodesR
import kotlinx.coroutines.flow.Flow

class ScanSetRepositoryImpl(private val dao: ScanSetDao) : ScanSetRepository {
    override suspend fun deleteScanSet(scanSet: ScanSetEntity) {
        dao.deleteScanSet(scanSet)
    }
    override suspend fun updateScanSet(scanSet: ScanSetEntity) {
        dao.updateScanSet(scanSet)
    }
    override suspend fun getScanSetById(id: Long): ScanSetEntity {
        return dao.getScanSetById(id)
    }
    override fun getScanSets(): Flow<List<ScanSetEntity>> {
        return dao.getScanSets()
    }
    override suspend fun insertScanSet(scanSet: ScanSetEntity) : Long {
        return dao.insertScanSet(scanSet)
    }

    override fun decodeScanCodes(encodedCodes: String): List<ScanCode> {
        return decodeScanCodesR(encodedCodes)
    }

    override fun encodeScanCodes(codes: List<ScanCode>): String {
        return encodeScanCodesR(codes)
    }
}