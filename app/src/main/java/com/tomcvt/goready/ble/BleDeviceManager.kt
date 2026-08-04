package com.tomcvt.goready.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger


private const val TAG = "BleDeviceManager"
// ---- Public data/state types ----
data class SavedDevice(val address: String, val name: String?)

data class DiscoveredDevice(
    val device: BluetoothDevice,
    val name: String?,
    val rssi: Int,
    val deviceType: Int?
)

sealed class BleConnectionState {
    object Disconnected : BleConnectionState()
    object Connecting : BleConnectionState()
    object Connected : BleConnectionState()
}

sealed class BleEvent {
    object Connected : BleEvent()
    data class Disconnected(val unexpected: Boolean) : BleEvent()
    data class Error(val message: String) : BleEvent()
}

data class DeviceConnection(
    val savedDevice: SavedDevice?,
    val connectionState: BleConnectionState,
) {
    val isConnected : Boolean get() = savedDevice != null && connectionState is BleConnectionState.Connected
}


// ---- Permission helper (min SDK 24 branch) ----
fun hasBlePermissions(context: Context): Boolean = if (Build.VERSION.SDK_INT >= 31) {
    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PERMISSION_GRANTED
} else {
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PERMISSION_GRANTED
}
/*
sealed class BleWarning {
    data class RequestFailed(val action: String, val alarmId: Int, val message: String?) : BleWarning()
}

private val _warnings = MutableSharedFlow<BleWarning>(
    replay = 0,
    extraBufferCapacity = 4,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
)
val warnings: SharedFlow<BleWarning> = _warnings.asSharedFlow()

fun emitWarning(warning: BleWarning) {
    _warnings.tryEmit(warning)
}
 */

sealed class ServiceMessages{
    //data class Success(val message: String) : ServiceMessages()
    data class Reconnected(val message: String) : ServiceMessages()
    data class Disconnected(val message: String) : ServiceMessages()
}
// ---- The manager ----
open class BleDeviceManager(
    private val passedContext: Context,
    private val bluetoothAdapter: BluetoothAdapter?
) {
    protected val context = passedContext.applicationContext
    private val prefs = context.getSharedPreferences("ble_prefs", Context.MODE_PRIVATE)
    protected val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val notificationManager = BleNotificationManager(context)

    // --- Public state ---
    protected val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    val connectionState: StateFlow<BleConnectionState> = _connectionState

    private val _savedDevice = MutableStateFlow(loadSavedDevice())
    val savedDevice: StateFlow<SavedDevice?> = _savedDevice

    val deviceConnectionState: StateFlow<DeviceConnection> = combine(savedDevice, connectionState) { saved, conn ->
        DeviceConnection(saved, conn)
    }.stateIn(scope, SharingStarted.Eagerly, DeviceConnection(loadSavedDevice(), BleConnectionState.Disconnected))



    private val _scanResults = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val scanResults: StateFlow<List<DiscoveredDevice>> = _scanResults

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _events = MutableSharedFlow<BleEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<BleEvent> = _events

    protected val _alarmActivityEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val alarmActivityEvents: SharedFlow<String> = _alarmActivityEvents


    // Device-initiated pushes with no matching request id (e.g. spontaneous "RINGING")
    private val _deviceEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val deviceEvents: SharedFlow<String> = _deviceEvents

    protected val _serviceMessages = MutableSharedFlow<ServiceMessages>(
        replay = 0,
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val serviceMessages: SharedFlow<ServiceMessages> = _serviceMessages
    fun emitServiceMessage(message: ServiceMessages) {
        _serviceMessages.tryEmit(message)
    }

    // --- Internal connection state ---
    private var gatt: BluetoothGatt? = null
    private var rxChar: BluetoothGattCharacteristic? = null
    private var txChar: BluetoothGattCharacteristic? = null
    private var intentionalDisconnect = false
    private var retryCount = 0
    private val maxDirectRetries = 3

    // --- RPC correlation ---
    private val pendingRpc = ConcurrentHashMap<Int, CompletableDeferred<String>>()
    private val rpcIdCounter = AtomicInteger(0)

    // init tries to autoconnect on creation
    init {

    }

    private val scanCallback = object : ScanCallback() {
        private val seen = mutableMapOf<String, DiscoveredDevice>()
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val mfg = result.scanRecord?.getManufacturerSpecificData(BleConstants.MANUFACTURER_ID)
            val deviceType = mfg?.getOrNull(2)?.toInt()
            if (hasBlePermissions(context)) {
                seen[result.device.address] =
                    DiscoveredDevice(result.device, result.device.name, result.rssi, deviceType)
            } else {
                Log.w(TAG, "Missing BLE permissions, scan callback")
                _events.tryEmit(BleEvent.Error("Missing BLE permissions"))
            }
            _scanResults.value = seen.values.sortedByDescending { it.rssi }
        }
        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
            _events.tryEmit(BleEvent.Error("Scan failed: $errorCode"))
        }
        fun reset() { seen.clear() }
    }

    // ---------------- Scanning ----------------

    @SuppressLint("MissingPermission")
    fun startScan(timeoutMs: Long = 8000) {
        if (!hasBlePermissions(context)) {
            _events.tryEmit(BleEvent.Error("Missing BLE permissions"))
            return
        }
        if (_isScanning.value) return
        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: run {
            _events.tryEmit(BleEvent.Error("Bluetooth is off or not supported"))
            return
        }
        scanCallback.reset()
        _scanResults.value = emptyList()
        _isScanning.value = true

        val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID)).build()
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        scanner.startScan(listOf(filter), settings, scanCallback)

        scope.launch { delay(timeoutMs); stopScan() }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!_isScanning.value) return
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        _isScanning.value = false
    }

    // ---------------- Connection lifecycle ----------------

    fun connectAndSave(device: BluetoothDevice) {
        var saved: SavedDevice?
        try {
            saved = SavedDevice(device.address, device.name)
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing permission", e)
            _events.tryEmit(BleEvent.Error("Security exception: $e"))
            return
        }
        prefs.edit().putString(KEY_ADDRESS, saved?.address).putString(KEY_NAME, saved?.name).apply()
        _savedDevice.value = saved
        intentionalDisconnect = false
        retryCount = 0
        connect(device, autoConnect = false)
    }

    fun connectToSaved() {
        val saved = _savedDevice.value ?: return
        bluetoothAdapter?.getRemoteDevice(saved.address)?.let {
            connectAndSave(it)
        }
    }

    @SuppressLint("MissingPermission")
    fun tryAutoConnect() {
        if (_connectionState.value !is BleConnectionState.Disconnected) return
        val saved = _savedDevice.value ?: return
        if (!hasBlePermissions(context)) return
        runCatching { bluetoothAdapter?.getRemoteDevice(saved.address) }.getOrNull()?.let {
            intentionalDisconnect = false
            retryCount = 0
            connect(it, autoConnect = false)
            Log.d(TAG, "Auto-connecting to saved device ${saved.address}")
        }
    }

    @SuppressLint("MissingPermission")
    open suspend fun requestAndReturnAutoConnect() : Result<String> {
        if (_connectionState.value !is BleConnectionState.Disconnected) return Result.success("Already connected/connecting")
        val saved = _savedDevice.value ?: return Result.failure(Exception("No saved device"))
        if (!hasBlePermissions(context)) return Result.failure(Exception("Missing BLE permissions"))
        runCatching { bluetoothAdapter?.getRemoteDevice(saved.address) }.getOrNull()?.let {
            intentionalDisconnect = false
            retryCount = 0
            connect(it, autoConnect = false)
            Log.d(TAG, "Auto-connecting to saved device ${saved.address}")
        }
        var result = Result.failure<String>(Exception("Timed out waiting for auto-connect"))
        val intervals = listOf(500L, 1000L, 2000L)
        try {
            withTimeout(5000L) {
                for (interval in intervals) {
                    delay(interval)
                    if (connectionState.value is BleConnectionState.Connected) {
                        result = Result.success("Auto-connected to saved device ${saved.address}")
                        break
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Timed out waiting for auto-connect")
        }
        return result
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        intentionalDisconnect = true
        gatt?.disconnect()
    }

    fun forgetDevice() {
        disconnect()
        prefs.edit().remove(KEY_ADDRESS).remove(KEY_NAME).apply()
        _savedDevice.value = null
    }

    private fun loadSavedDevice(): SavedDevice? {
        val address = prefs.getString(KEY_ADDRESS, null) ?: return null
        return SavedDevice(address, prefs.getString(KEY_NAME, null))
    }

    @SuppressLint("MissingPermission")
    private fun connect(device: BluetoothDevice, autoConnect: Boolean) {
        _connectionState.value = BleConnectionState.Connecting
        gatt = device.connectGatt(context, autoConnect, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    @SuppressLint("MissingPermission")
    private fun scheduleReconnect(device: BluetoothDevice) {
        retryCount++
        if (retryCount <= maxDirectRetries) {
            Log.d(TAG, "Reconnecting in ${2000L * retryCount}ms")
            scope.launch { delay(2000L * retryCount); connect(device, autoConnect = false) }
        } else {
            Log.d(TAG, "Reconnecting passively")
            connect(device, autoConnect = true) // let the OS reconnect passively when back in range
        }
    }

    private fun failAllPendingRpc(cause: Throwable) {
        pendingRpc.values.forEach { it.completeExceptionally(cause) }
        pendingRpc.clear()
    }

    // ---------------- GATT callback ----------------

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    retryCount = 0
                    g.requestMtu(185) // discoverServices() happens in onMtuChanged
                    Log.d(TAG, "Connected to ${g.device.address}")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    val wasIntentional = intentionalDisconnect
                    _connectionState.value = BleConnectionState.Disconnected
                    failAllPendingRpc(IOException("BLE disconnected"))
                    _events.tryEmit(BleEvent.Disconnected(unexpected = !wasIntentional))
                    g.close()
                    val dcDevice = g.device
                    gatt = null
                    if (wasIntentional) {
                        notificationManager.cancelNotification()
                        Log.d(TAG, "Disconnected intentionally from ${dcDevice.address}")
                    } else {
                        notificationManager.notifyDisconnected(dcDevice.address)
                        scheduleReconnect(dcDevice)
                        Log.d(TAG, "Disconnected passively from ${dcDevice.address}")
                    }
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
            g.discoverServices()
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            val service = g.getService(BleConstants.SERVICE_UUID)
            if (service == null) {
                _events.tryEmit(BleEvent.Error("Service not found"))
                return
            }
            rxChar = service.getCharacteristic(BleConstants.CHAR_RX)
            txChar = service.getCharacteristic(BleConstants.CHAR_TX)
            txChar?.let {
                g.setCharacteristicNotification(it, true)
                it.getDescriptor(BleConstants.CCCD_UUID)?.let { d ->
                    d.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    g.writeDescriptor(d)
                }
            }
            _connectionState.value = BleConnectionState.Connected
            _events.tryEmit(BleEvent.Connected)
            notificationManager.notifyConnected(g.device.address)
        }

        // pre-API 33 path
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (Build.VERSION.SDK_INT >= 33) return // API 33+ devices use the overload below instead
            if (characteristic.uuid == BleConstants.CHAR_TX) {
                characteristic.value?.let { onIncoming(it.toString(Charsets.UTF_8)) }
            }
        }
        // API 33+ path
        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            if (characteristic.uuid == BleConstants.CHAR_TX) {
                onIncoming(value.toString(Charsets.UTF_8))
            }
        }
    }

    private fun onIncoming(raw: String) {
        val sep = raw.indexOf(':')
        //we need to handle generic ACK instead of id and log it
        val generic = raw.substring(0, sep)
        if (generic == "ACK") {
            Log.d(TAG, "ACK received: $raw ")
            return
        }
        val id = if (sep > 0) raw.substring(0, sep).toIntOrNull() else null
        if (id != null) {
            val resolved = pendingRpc.remove(id)
            if (resolved != null) {
                resolved.complete(raw.substring(sep + 1))
                return
            }
        }
        Log.d(TAG, "Unhandled message: $raw")
        _deviceEvents.tryEmit(raw) // no matching request — unsolicited push
    }

    // ---------------- Outgoing commands ----------------

    @SuppressLint("MissingPermission")
    fun sendCommand(command: String) {
        val char = rxChar ?: return
        val g = gatt ?: return
        if (Build.VERSION.SDK_INT >= 33) {
            g.writeCharacteristic(char, command.toByteArray(Charsets.UTF_8), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        } else {
            @Suppress("DEPRECATION")
            char.value = command.toByteArray(Charsets.UTF_8)
            @Suppress("DEPRECATION")
            g.writeCharacteristic(char)
        }
    }

    open suspend fun request(command: String, timeoutMs: Long = 3000): Result<String> {
        val id = rpcIdCounter.incrementAndGet() and 0xFF
        val deferred = CompletableDeferred<String>()
        pendingRpc[id] = deferred
        //we need to change it a bit so we add an event to alarm activity events about success/failure
        var result : Result<String>
        try {
            Log.d(TAG, "Sending RPC $id: $command")
            withTimeout(timeoutMs) {
                sendCommand("$id:$command")
                val res = deferred.await()
                result = Result.success(res)
                //_alarmActivityEvents.tryEmit
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "RPC $id timed out")
            result = Result.failure(e)
        } finally {
            pendingRpc.remove(id)
        }
        return result
    }
    // utility methods

    suspend fun awaitTryAutoConnectIfNotConnected() : Result<String> {
        if (connectionState.value is BleConnectionState.Connected) {
            return Result.success("Already connected")
        }
        return requestAndReturnAutoConnect()
    }

    open suspend fun requestStartAlarm(alarmId: Int) : Result<String> {
        val command = encodeAlarmPlayCommand(alarmId)
        val res = request(command)
        if (res.isFailure) {
            _alarmActivityEvents.tryEmit("ERROR: $res")
        }
        return res
    }

    open suspend fun requestStopAlarm(alarmId: Int) : Result<String> {
        val command = encodeAlarmStopCommand(alarmId)
        val res = request(command)
        if (res.isFailure) {
            _alarmActivityEvents.tryEmit("ERROR: $res")
        }
        return res
    }

    open suspend fun requestSnoozeAlarm(alarmId: Int, durationSeconds: Int = 5) : Result<String> {
        val command = encodeAlarmSnoozeCommand(alarmId, durationSeconds)
        val res = request(command)
        if (res.isFailure) {
            _alarmActivityEvents.tryEmit("ERROR: $res")
        }
        return res
    }


    /**
    |-------------------------------------|--------------------------------------|
| `ALARM:SET:<alarmId>:OK`            | Alarm created                        |
| `ALARM:SET:<alarmId>:UPDATED`       | Existing alarm with same ID replaced |
| `ALARM:SET:<alarmId>:DUPLICATE_TIME`| Another alarm already at that time   |
| `ALARM:SET:<alarmId>:NO_SPACE`      | Alarm table full                     |
     */
    open suspend fun requestSetAlarmInSync(alarmId: Int, hour: Int, minute: Int, message: String, daysOfWeek: String) : SingleSyncResult {
        val command = encodeAlarmSetCommand(alarmId, hour, minute, message, daysOfWeek)
        val res = request(command)
        if (res.isFailure) {
            return SingleSyncResult.ConError(-1, "ERROR: $res")
        }
        val raw = res.getOrNull()
        val resStatus = raw?.split(":")?.get(3) // 3 is the status? TODO: check
        Log.d(TAG, "Sync alarm $alarmId response: $resStatus")
        return when (resStatus) {
            "OK" -> SingleSyncResult.Ok(alarmId, raw)
            "UPDATED" -> SingleSyncResult.Updated(alarmId, raw)
            "DUPLICATE_TIME" -> SingleSyncResult.DuplicateTime(alarmId, raw)
            "NO_SPACE" -> SingleSyncResult.NoSpace(alarmId, raw)
            else -> SingleSyncResult.Error(alarmId, "Unknown response: $raw")
        }
    }

    /**
     *
     */
    open suspend fun requestSyncFresh(epochSeconds: Long) : SyncRequestResult {
        val command = encodeSyncFreshCommand(epochSeconds)
        val res = request(command)
        if (res.isFailure) {
            return SyncRequestResult.ConError("ERROR: $res")
        }
        val raw = res.getOrNull()
        val resStatus = raw?.split(":")?.get(1)
        Log.d(TAG, "Sync fresh response: $resStatus")
        return when (resStatus) {
            "OPEN" -> SyncRequestResult.Open(raw)
            "BUSY" -> SyncRequestResult.Busy(raw)
            else -> SyncRequestResult.Error("Unknown response: $raw")
        }
    }






    fun encodeAlarmPlayCommand(alarmId: Int): String { return "ALARM:PLAY:$alarmId" }
    fun encodeAlarmStopCommand(alarmId: Int): String { return "ALARM:STOP:$alarmId" }
    fun encodeAlarmSnoozeCommand(alarmId: Int, durationSeconds: Int): String { return "ALARM:SNOOZE:$alarmId:$durationSeconds" }

    fun encodeAlarmSetCommand(alarmId: Int, hour: Int, minute: Int, message: String, daysOfWeek: String): String {
        return "ALARM:SET:$alarmId:$hour:$minute:$message:$daysOfWeek"
    }
    fun encodeSyncSingleCommand(epochSeconds: Long): String { return "SYNC:SINGLE:$epochSeconds" }
    fun encodeSyncFreshCommand(epochSeconds: Long): String { return "SYNC:FRESH:$epochSeconds" }
    fun encodeSyncEndCommand(): String { return "SYNC:END" }

    open suspend fun requestTimeSync(): Result<String> {
        val command = "TIME:${epochSeconds()}:${localOffsetSeconds()}"
        return request(command)
    }


    fun closeNotifications() {
        notificationManager.cancelNotification()
    }

    private fun epochSeconds() {
        System.currentTimeMillis() / 1000
    }

    private fun localOffsetSeconds() {
        TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000
    }

    sealed class SingleSyncResult(val id: Int, val response: String) {
        class Ok(id: Int, response: String) : SingleSyncResult(id, response)
        class Updated(id: Int, response: String) : SingleSyncResult(id, response)
        class DuplicateTime(id: Int, response: String) : SingleSyncResult(id, response)
        class NoSpace(id: Int, response: String) : SingleSyncResult(id, response)
        class Error(id: Int, response: String) : SingleSyncResult(id, response)
        class ConError(id: Int, response: String) : SingleSyncResult(id, response)
    }

    sealed class SyncRequestResult(val response: String) {
        class Open(response: String) : SyncRequestResult(response)
        class Busy(response: String) : SyncRequestResult(response)
        class Error(response: String) : SyncRequestResult(response)
        class ConError(response: String) : SyncRequestResult(response)
    }

    sealed class SyncEndResult(val response: String, val epochSeconds: Long = 0L) {
        class Ok(response: String, epochSeconds: Long) : SyncEndResult(response, epochSeconds)
        class Error(response: String) : SyncEndResult(response)
    }

    companion object {
        private const val KEY_ADDRESS = "saved_device_address"
        private const val KEY_NAME = "saved_device_name"
    }
    //TODO: add to activieties cancelNotification on ondestroy
}

/*
### ALARM:STOP – Stop a ringing alarm

```
<id>:ALARM:STOP:<alarmId>
```

**Response:** `<id>:<alarmId>:OK`
`<id>:<alarmId>:ERROR`

---

### ALARM:CLEAR – Delete an alarm

```
<id>:ALARM:CLEAR:<alarmId>
<id>:ALARM:CLEAR:ALL
```

**Response:** `<id>:OK`

---

### ALARM:PLAY – Force-play an alarm immediately

```
<id>:ALARM:PLAY:<alarmId>
```

**Responses:**

| Notification              | Meaning                                      |
|---------------------------|----------------------------------------------|
| `<id>:<alarmId>:OK`       | Alarm found and started                      |
| `<id>:<alarmId>:NOT_FOUND_OK` | ID not in table; default sound played, ID tracked so STOP still works |
| `<id>:<alarmId>:ERROR`    | Internal error                               |

---

### ALARM:SNOOZE – Snooze the currently playing alarm

```
<id>:ALARM:SNOOZE:<alarmId>:<durationSeconds>
```

Stops audio for `durationSeconds`, then resumes automatically.

**Responses:**

| Notification         | Meaning                                       |
|----------------------|-----------------------------------------------|
| `<id>:<alarmId>:OK`  | Alarm was playing and is now snoozed          |
| `<id>:<alarmId>:ERROR` | Alarm with that ID is not currently playing |

-----------------------------

### ALARM:SET – Create / update an alarm

```
<id>:ALARM:SET:<alarmId>:<hour>:<minute>:<message>:<daysOfWeek>
```

| Field        | Description                                                        |
|--------------|--------------------------------------------------------------------|
| `alarmId`    | Unique alarm identifier (uint32, > 0)                              |
| `hour`       | 0–23                                                               |
| `minute`     | 0–59                                                               |
| `message`    | Display label (no colons)                                          |
| `daysOfWeek` | 7-char string `MTWTFSS` order, `1` = active (parsed but currently not used – stored for future use) |

> **Note:** The firmware currently schedules alarms as one-time events regardless of `daysOfWeek`.

**Responses** (sent as a *separate* notification, not the generic ack):

| Notification                        | Meaning                              |
|-------------------------------------|--------------------------------------|
| `ALARM:SET:<alarmId>:OK`            | Alarm created                        |
| `ALARM:SET:<alarmId>:UPDATED`       | Existing alarm with same ID replaced |
| `ALARM:SET:<alarmId>:DUPLICATE_TIME`| Another alarm already at that time   |
| `ALARM:SET:<alarmId>:NO_SPACE`      | Alarm table full                     |

**Example:**
```
1:ALARM:SET:3:7:30:Wake up:1111100
```

For use during sync

---

### SYNC:FRESH – Begin a full alarm sync

```
<id>:SYNC:FRESH:<timestamp>
```

Starts a sync session and clears all existing alarms; the client is then expected to send its full alarm set via `ALARM:SET` commands, finishing with `SYNC:END`.

| Field       | Description                                  |
|-------------|-----------------------------------------------|
| `timestamp` | Client-chosen sync start time (unix epoch)   |

**Responses:**

| Notification  | Meaning                                   |
|---------------|--------------------------------------------|
| `<id>:SYNC:OPEN`  | Sync session started, alarms cleared     |
| `<id>:SYNC:BUSY`  | A sync is already in progress             |
| `<id>:SYNC:ERROR` | Missing/invalid `timestamp`               |

---

### SYNC:SINGLE – Begin a single-alarm sync

```
<id>:SYNC:SINGLE:<timestamp>
```

Starts a sync session without clearing existing alarms; used when the client only needs to push one or a few `ALARM:SET` updates, finishing with `SYNC:END`.

**Responses:**

| Notification  | Meaning                                   |
|---------------|--------------------------------------------|
| `<id>:SYNC:OPEN`  | Sync session started                       |
| `<id>:SYNC:BUSY`  | A sync is already in progress             |
| `<id>:SYNC:ERROR` | Missing/invalid `timestamp`               |

---

### SYNC:END – Close the current sync session

```
<id>:SYNC:END
```

Persists all alarms set during the session and clears the in-progress state.

**Responses:**

| Notification              | Meaning                                     |
|---------------------------|----------------------------------------------|
| `<id>:SYNC:OK:<timestamp>`| Sync finished; `timestamp` is the value passed to the opening `SYNC:FRESH`/`SYNC:SINGLE` |
| `<id>:SYNC:ERROR`         | No sync was in progress                     |

---

### SYNC:CHECK – Query the last successful sync

```
<id>:SYNC:CHECK
```

**Response:** `<id>:SYNC:CHECK:<timestamp>` – `timestamp` is the value from the last completed `SYNC:END` (`0` if none yet).

---

### TIME – Set clock

```
<id>:TIME:<epochSeconds>:<utcOffsetSeconds>
```

| Field              | Description                                     |
|--------------------|-------------------------------------------------|
| `epochSeconds`     | Unix timestamp (UTC)                            |
| `utcOffsetSeconds` | Local UTC offset in seconds (e.g. `3600` = +1h) |

**Responses:**
- `ACK:TIME` – sent immediately after the clock is updated *(no requestId prefix on this one)* (removed)
- `<id>:OK` – generic ack that follows

**Example:**
```
42:TIME:1751000000:7200
```

 */