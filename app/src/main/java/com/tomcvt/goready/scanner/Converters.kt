package com.tomcvt.goready.scanner


fun encodeScanCodesR(codes: List<ScanCode>): String {
    if (codes.isEmpty()) return ""
    return codes.joinToString(separator = ",") { "${it.barcode}:${it.tip}" }
}

fun decodeScanCodesR(encoded: String): List<ScanCode> {
    if (encoded.isEmpty()) return emptyList()
    return encoded.split(",").map {
        val (barcode, tip) = it.split(":")
        ScanCode(barcode, tip)
    }
}