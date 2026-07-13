package com.tomcvt.goready.scanner

/**
 * Data class representing a barcode scan.
 * List encoded as a string, separated by commas.
 * "barcode1:tip1,barcode2:tip2"
 */
data class ScanCode(
    val barcode: String,
    val tip: String
)
