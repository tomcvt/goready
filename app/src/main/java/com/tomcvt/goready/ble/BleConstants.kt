package com.tomcvt.goready.ble

import java.util.UUID

object BleConstants {
    val SERVICE_UUID: UUID = UUID.fromString("12345678-1234-1234-1234-1234567890ab")
    val CHAR_RX: UUID = UUID.fromString("12345678-1234-1234-1234-1234567890ac") // write
    val CHAR_TX: UUID = UUID.fromString("12345678-1234-1234-1234-1234567890ad") // notify
    val CHAR_INFO: UUID = UUID.fromString("12345678-1234-1234-1234-1234567890ae") // read
    val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    const val MANUFACTURER_ID = 0xFFFF
}