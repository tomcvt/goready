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

// ---- The manager ----
class BleDeviceManager(
    private val passedContext: Context,
    private val bluetoothAdapter: BluetoothAdapter
) {
    private val context = passedContext.applicationContext
    private val prefs = context.getSharedPreferences("ble_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val notificationManager = BleNotificationManager(context)

    // --- Public state ---
    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
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

    private val _alarmActivityEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val alarmActivityEvents: SharedFlow<String> = _alarmActivityEvents


    // Device-initiated pushes with no matching request id (e.g. spontaneous "RINGING")
    private val _deviceEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val deviceEvents: SharedFlow<String> = _deviceEvents

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
        val scanner = bluetoothAdapter.bluetoothLeScanner ?: run {
            _events.tryEmit(BleEvent.Error("Bluetooth is off"))
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
        bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
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
        connectAndSave(bluetoothAdapter.getRemoteDevice(saved.address))
    }

    @SuppressLint("MissingPermission")
    fun tryAutoConnect() {
        if (_connectionState.value !is BleConnectionState.Disconnected) return
        val saved = _savedDevice.value ?: return
        if (!hasBlePermissions(context)) return
        runCatching { bluetoothAdapter.getRemoteDevice(saved.address) }.getOrNull()?.let {
            intentionalDisconnect = false
            retryCount = 0
            connect(it, autoConnect = false)
            Log.d(TAG, "Auto-connecting to saved device ${saved.address}")
        }
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
            @Suppress("DEPRECATION") char.value = command.toByteArray(Charsets.UTF_8)
            @Suppress("DEPRECATION") g.writeCharacteristic(char)
        }
    }

    suspend fun request(command: String, timeoutMs: Long = 3000): Result<String> {
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

    suspend fun requestStartAlarm(alarmId: Int) : Result<String> {
        val command = encodeAlarmPlayCommand(alarmId)
        val res = request(command)
        when (res) {
            Result.success(command) -> {
                //for now nothing
            }
            else -> {
                _alarmActivityEvents.tryEmit("ERROR: $res")
            }
        }
        return res
    }

    suspend fun requestStopAlarm(alarmId: Int) : Result<String> {
        val command = encodeAlarmStopCommand(alarmId)
        val res = request(command)
        when (res) {
            Result.success(command) -> {
                //for now nothing
            }
            else -> {
                _alarmActivityEvents.tryEmit("ERROR: $res")
            }
        }
        return res
    }

    suspend fun requestSnoozeAlarm(alarmId: Int, durationSeconds: Int = 5) : Result<String> {
        val command = encodeAlarmSnoozeCommand(alarmId, durationSeconds)
        val res = request(command)
        when (res) {
            Result.success(command) -> {
                //for now nothing
            }
            else -> {
                _alarmActivityEvents.tryEmit("ERROR: $res")
            }
        }
        return res
    }


    fun encodeAlarmPlayCommand(alarmId: Int): String { return "ALARM:PLAY:$alarmId" }
    fun encodeAlarmStopCommand(alarmId: Int): String { return "ALARM:STOP:$alarmId" }
    fun encodeAlarmSnoozeCommand(alarmId: Int, durationSeconds: Int): String { return "ALARM:SNOOZE:$alarmId:$durationSeconds" }


    fun closeNotifications() {
        notificationManager.cancelNotification()
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

 */