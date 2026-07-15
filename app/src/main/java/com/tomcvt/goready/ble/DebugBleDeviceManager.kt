package com.tomcvt.goready.ble

import android.content.Context
import android.util.Log
import kotlinx.coroutines.delay

class DebugBleDeviceManager(
    context: Context
) : BleDeviceManager(context, null) {

    private var mockConnected = false

    override suspend fun requestAndReturnAutoConnect(): Result<String> {
        delay(500) // simulate work
        mockConnected = true
        _connectionState.value = BleConnectionState.Connected
        return Result.success("Mock: Connected to debug device")
    }

    override suspend fun request(command: String, timeoutMs: Long): Result<String> {
        return if (mockConnected) {
            Result.success("Mock OK: $command")
        } else {
            Result.failure(Exception("Mock: Not connected"))
        }
    }

    override suspend fun requestStartAlarm(alarmId: Int): Result<String> {
        _alarmActivityEvents.tryEmit("DEBUG: Starting alarm $alarmId")
        return request(encodeAlarmPlayCommand(alarmId))
    }

    override suspend fun requestStopAlarm(alarmId: Int): Result<String> {
        _alarmActivityEvents.tryEmit("DEBUG: Stopping alarm $alarmId")
        return request(encodeAlarmStopCommand(alarmId))
    }

    override suspend fun requestSnoozeAlarm(alarmId: Int, durationSeconds: Int): Result<String> {
        // Toggle state so service messages are emitted
        mockConnected = !mockConnected
        
        return if (mockConnected) {
            _connectionState.value = BleConnectionState.Connected
            _alarmActivityEvents.tryEmit("DEBUG: Snoozed (Now Connected)")
            Result.success("Mock: Snoozed")
        } else {
            _connectionState.value = BleConnectionState.Disconnected
            _alarmActivityEvents.tryEmit("DEBUG: Snoozed (Now Disconnected)")
            Result.failure(Exception("Mock: Snooze failed - Disconnected"))
        }
    }
}