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
import android.bluetooth.BluetoothStatusCodes
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
import com.tomcvt.goready.data.AlarmEntity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

sealed class BleUiConnectionState {
    object Disconnected : BleUiConnectionState()
    object Connecting : BleUiConnectionState()
    object Connected : BleUiConnectionState()
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

    protected val _bleUiConnectionState = MutableStateFlow<BleUiConnectionState>(BleUiConnectionState.Disconnected)
    val bleUiConnectionState: StateFlow<BleUiConnectionState> = _bleUiConnectionState

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

    private val _verboseEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val verboseEvents: SharedFlow<String> = _verboseEvents

    protected val _serviceMessages = MutableSharedFlow<ServiceMessages>(
        replay = 0,
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val serviceMessages: SharedFlow<ServiceMessages> = _serviceMessages
    fun emitServiceMessage(message: ServiceMessages) {
        _serviceMessages.tryEmit(message)
    }

    private val _lastSyncEsp: MutableStateFlow<Long> = MutableStateFlow(prefs.getLong("last_sync_esp", 0L))
    val lastSyncEsp: StateFlow<Long> = _lastSyncEsp

    fun setLastSyncEsp(epochSeconds: Long) {
        _lastSyncEsp.value = epochSeconds
        prefs.edit().putLong("last_sync_esp", epochSeconds).apply()
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
        teardownGatt()
        intentionalDisconnect = false
        retryCount = 0
        _bleUiConnectionState.value = BleUiConnectionState.Connecting
        connect(device, autoConnect = false)
    }

    fun connectToSaved(): Boolean {
        val saved = _savedDevice.value ?: return false
        bluetoothAdapter?.getRemoteDevice(saved.address)?.let {
            connectAndSave(it)
            return true
        }
        return true
    }

    @SuppressLint("MissingPermission")
    fun tryAutoConnect() {
        if (_connectionState.value !is BleConnectionState.Disconnected) return
        val saved = _savedDevice.value ?: return
        if (!hasBlePermissions(context)) return
        runCatching { bluetoothAdapter?.getRemoteDevice(saved.address) }.getOrNull()?.let {
            intentionalDisconnect = false
            retryCount = 0
            _bleUiConnectionState.value = BleUiConnectionState.Connecting
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
            _bleUiConnectionState.value = BleUiConnectionState.Connecting
            connect(it, autoConnect = false)
            Log.d(TAG, "Auto-connecting to saved device ${saved.address}")
        }
        return try {
            withTimeout(5000L) {
                connectionState.filter { it is BleConnectionState.Connected }.first()
            }
            Result.success("Connected")
        } catch (e: TimeoutCancellationException) {
            _bleUiConnectionState.value = BleUiConnectionState.Disconnected
            Log.w(TAG, "Timed out waiting for auto-connect")
            Result.failure(e)
        } catch (e: Exception) {
            _bleUiConnectionState.value = BleUiConnectionState.Disconnected
            Log.w(TAG, "Failed to auto-connect: ${e.message}")
            Result.failure(e)
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
            _connectionState.value = BleConnectionState.Disconnected
            _bleUiConnectionState.value = BleUiConnectionState.Disconnected
            Log.d(TAG, "Reconnecting passively")
            connect(device, autoConnect = true) // let the OS reconnect passively when back in range
        }
    }

    private fun failAllPendingRpc(cause: Throwable) {
        pendingRpc.values.forEach { it.completeExceptionally(cause) }
        pendingRpc.clear()
    }

    private val gattOpMutex = Mutex()
    private var pendingGattOp: CompletableDeferred<Int>? = null

    private var connectionSetupJob: Job? = null

    private fun failPendingGattOp() {
        pendingGattOp?.let { if (!it.isCompleted) it.complete(GATT_STATUS_LOCAL_FAILURE) }
        pendingGattOp = null
    }

    // ---------------- GATT callback ----------------

    private val gattCallback = object : BluetoothGattCallback() {

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        // Rare: STATE_CONNECTED fired alongside a non-success status. Treat as a failed
                        // attempt rather than a real connection — tear down and let retry logic decide.
                        Log.w(TAG, "Connected callback with bad status=$status (${gattStatusName(status)})")
                        _verboseEvents.tryEmit("BLE connect failed: status=$status (${gattStatusName(status)})")
                        g.close()
                        val dcDevice = g.device
                        gatt = null
                        _connectionState.value = BleConnectionState.Disconnected
                        _bleUiConnectionState.value = BleUiConnectionState.Disconnected
                        if (!intentionalDisconnect) scheduleReconnect(dcDevice)
                        return
                    }
                    retryCount = 0
                    Log.d(TAG, "Connected to ${g.device.address}, starting setup")
                    connectionSetupJob?.cancel() // defensive: shouldn't normally be non-null here, but cheap insurance
                    connectionSetupJob = scope.launch { runConnectionSetup(g) }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    val wasIntentional = intentionalDisconnect
                    val statusName = gattStatusName(status)
                    Log.d(TAG, "Disconnected: status=$status ($statusName), wasIntentional=$wasIntentional")
                    _verboseEvents.tryEmit("BLE disconnected: status=$status ($statusName)")

                    _connectionState.value = BleConnectionState.Disconnected
                    _bleUiConnectionState.value = BleUiConnectionState.Disconnected
                    failAllPendingRpc(IOException("BLE disconnected (status=$status)"))
                    failPendingGattOp()
                    connectionSetupJob?.cancel() // stop any in-flight setup for this same connection cleanly
                    connectionSetupJob = null
                    g.close()
                    val dcDevice = g.device
                    gatt = null

                    // A peer-initiated GRACEFUL disconnect (19) means the device itself chose to
                    // close the link while both sides were awake — treat it like an intentional
                    // disconnect and don't auto-retry. NOTE: deep sleep does NOT produce this status
                    // (the radio just goes silent, which shows up as CONN_TIMEOUT/8 instead) — so this
                    // does not interfere with reconnecting to a device that woke back up. See the
                    // summary doc for more on this distinction if you want to change the behavior.
                    val peerGraceful = status == GATT_STATUS_CONN_TERMINATE_PEER_USER
                    val treatAsIntentional = wasIntentional || peerGraceful

                    _events.tryEmit(BleEvent.Disconnected(unexpected = !treatAsIntentional))

                    if (treatAsIntentional) {
                        notificationManager.cancelNotification()
                        if (peerGraceful && !wasIntentional) {
                            Log.d(TAG, "Device closed the connection gracefully, not retrying: ${dcDevice.address}")
                        } else {
                            Log.d(TAG, "Disconnected intentionally from ${dcDevice.address}")
                        }
                    } else {
                        // Anything else — timeout (8), generic error (133), or unrecognized — retry.
                        notificationManager.notifyDisconnected(dcDevice.address)
                        Log.d(TAG, "Disconnected unexpectedly from ${dcDevice.address} ($statusName), retrying")
                        scheduleReconnect(dcDevice)
                    }
                }
            }
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

        override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
            pendingGattOp?.complete(status)
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            pendingGattOp?.complete(status)
        }

        override fun onDescriptorWrite(g: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            pendingGattOp?.complete(status)
        }

        override fun onCharacteristicWrite(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            pendingGattOp?.complete(status)
        }
    }
    //--------
    /**
     * Issues a single async GATT operation and suspends until its completion callback fires
     * (or times out). Returns the GATT status code, or GATT_STATUS_LOCAL_FAILURE (-1) if the
     * call couldn't even be issued.
     */
    private suspend fun awaitGattOp(
        opName: String,
        timeoutMs: Long = 5000,
        issue: () -> Boolean
    ): Int = gattOpMutex.withLock {
        val deferred = CompletableDeferred<Int>()
        pendingGattOp = deferred
        val issued = try {
            issue()
        } catch (e: SecurityException) {
            Log.w(TAG, "$opName threw SecurityException: ${e.message}")
            false
        }
        if (!issued) {
            Log.w(TAG, "$opName failed to issue")
            pendingGattOp = null
            return@withLock GATT_STATUS_LOCAL_FAILURE
        }
        val result = try {
            withTimeout(timeoutMs) { deferred.await() }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "$opName timed out after ${timeoutMs}ms")
            GATT_STATUS_LOCAL_FAILURE
        }
        pendingGattOp = null
        result
    }

    @SuppressLint("MissingPermission")
    private suspend fun runConnectionSetup(g: BluetoothGatt) {
        val mtuStatus = awaitRequestMtu(g, 185)
        if (mtuStatus != BluetoothGatt.GATT_SUCCESS) {
            // Not fatal — proceed with the default 23-byte MTU rather than aborting the connection.
            Log.w(TAG, "MTU negotiation failed: status=$mtuStatus (${gattStatusName(mtuStatus)}), continuing")
            _verboseEvents.tryEmit("MTU negotiation failed (status=$mtuStatus), continuing with default")
        }

        val discStatus = awaitDiscoverServices(g)
        if (discStatus != BluetoothGatt.GATT_SUCCESS) {
            Log.w(TAG, "Service discovery failed: status=$discStatus (${gattStatusName(discStatus)})")
            _verboseEvents.tryEmit("Critical: service discovery failed (status=$discStatus)")
            _events.tryEmit(BleEvent.Error("Service discovery failed: status=$discStatus"))
            g.disconnect() // onConnectionStateChange(DISCONNECTED) drives teardown + retry from here
            return
        }

        val service = g.getService(BleConstants.SERVICE_UUID)
        if (service == null) {
            _events.tryEmit(BleEvent.Error("Service not found"))
            _verboseEvents.tryEmit("Critical: expected service UUID missing after discovery")
            g.disconnect()
            return
        }
        rxChar = service.getCharacteristic(BleConstants.CHAR_RX)
        txChar = service.getCharacteristic(BleConstants.CHAR_TX)

        val cccd = txChar?.let {
            g.setCharacteristicNotification(it, true)
            it.getDescriptor(BleConstants.CCCD_UUID)
        }
        if (cccd != null) {
            val cccdStatus = awaitWriteDescriptor(g, cccd)
            if (cccdStatus != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "CCCD write failed: status=$cccdStatus (${gattStatusName(cccdStatus)})")
                _deviceEvents.tryEmit("CRITICAL: notification subscription failed (status=$cccdStatus)")
                _verboseEvents.tryEmit("Critical issue: notification subscription failed (status=$cccdStatus)")
                // We're connected but will never receive replies — not usable, so bail out.
                g.disconnect()
                return
            }
        } else {
            Log.w(TAG, "CCCD descriptor not found on TX characteristic — notifications unavailable")
            _verboseEvents.tryEmit("Critical issue: CCCD descriptor missing, notifications unavailable")
        }

        _connectionState.value = BleConnectionState.Connected
        _bleUiConnectionState.value = BleUiConnectionState.Connected
        _events.tryEmit(BleEvent.Connected)
        notificationManager.notifyConnected(g.device.address)
        initDevice()
    }

    @SuppressLint("MissingPermission")
    private suspend fun awaitRequestMtu(g: BluetoothGatt, mtu: Int): Int =
        awaitGattOp("requestMtu") { g.requestMtu(mtu) }

    @SuppressLint("MissingPermission")
    private suspend fun awaitDiscoverServices(g: BluetoothGatt): Int =
        awaitGattOp("discoverServices", timeoutMs = 8000) { g.discoverServices() }

    @SuppressLint("MissingPermission")
    private suspend fun awaitWriteDescriptor(g: BluetoothGatt, d: BluetoothGattDescriptor): Int =
        awaitGattOp("writeDescriptor(${d.uuid})") {
            if (Build.VERSION.SDK_INT >= 33) {
                g.writeDescriptor(d, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) == BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                d.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                g.writeDescriptor(d)
            }
        }

    @SuppressLint("MissingPermission")
    private suspend fun awaitWriteCharacteristic(g: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray): Int =
        awaitGattOp("writeCharacteristic(${c.uuid})") {
            if (Build.VERSION.SDK_INT >= 33) {
                g.writeCharacteristic(c, value, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                c.value = value
                @Suppress("DEPRECATION")
                g.writeCharacteristic(c)
            }
        }


    private fun initDevice() {
        scope.launch {
            val res = requestTimeSync()
            if (res.isFailure) {
                Log.w(TAG, "Failed to sync time: $res")
            } else {
                Log.d(TAG, "Synced time: $res")
            }
            getAndUpdateLastSync()
        }
    }

    @Deprecated("Bypasses the GATT op queue — a concurrent awaitGattOp caller can have its " +
            "completion stolen by this call's write-confirmation callback. Use request() instead, " +
            "which calls sendCommandAwaitingWrite() internally.")
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

    private suspend fun sendCommandAwaitingWrite(command: String): Boolean {
        val char = rxChar ?: return false
        val g = gatt ?: return false
        val status = awaitWriteCharacteristic(g, char, command.toByteArray(Charsets.UTF_8))
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.w(TAG, "writeCharacteristic failed: status=$status (${gattStatusName(status)})")
            _verboseEvents.tryEmit("Command write failed (status=$status): $command")
            return false
        }
        return true
    }

    private fun onIncoming(raw: String) {
        val sep = raw.indexOf(':')
        //we need to handle generic ACK instead of id and log it
        if (sep < 0) {
            Log.w(TAG, "Unhandled message: $raw")
            _verboseEvents.tryEmit("Unhandled message: $raw")
        }
        val generic = raw.substring(0, sep)
        if (generic == "ACK") {
            Log.d(TAG, "ACK received: $raw ")
            return
        } //TODO for removal after check
        val id = if (sep > 0) raw.substring(0, sep).toIntOrNull() else null
        if (id != null) {
            val resolved = pendingRpc.remove(id)
            if (resolved != null) {
                resolved.complete(raw.substring(sep + 1))
                return
            }
        }
        Log.w(TAG, "Unhandled message: $raw")
        //_deviceEvents.tryEmit(raw) // no matching request — unsolicited push //TODO: rethink
        _verboseEvents.tryEmit("Unhandled message: $raw")
    }

    // ---------------- Outgoing commands ----------------

    open suspend fun request(command: String, timeoutMs: Long = 3000): Result<String> {
        val id = rpcIdCounter.incrementAndGet() and 0xFF
        val deferred = CompletableDeferred<String>()
        pendingRpc[id] = deferred
        //we need to change it a bit so we add an event to alarm activity events about success/failure
        var result : Result<String>
        try {
            Log.d(TAG, "Sending RPC $id: $command")
            withTimeout(timeoutMs) {
                val written = sendCommandAwaitingWrite("$id:$command")
                if (!written) throw IOException("Write failed for RPC $id")
                val res = deferred.await()
                result = Result.success(res)
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "RPC $id timed out")
            result = Result.failure(e)
        } catch (e: Exception) {
            Log.w(TAG, "RPC $id failed: ${e.message}")
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

    open suspend fun requestSyncEnd() : SyncEndResult {
        val command = encodeSyncEndCommand()
        val res = request(command)
        if (res.isFailure) {
            return SyncEndResult.ConError("ERROR: $res")
        }
        val raw = res.getOrNull()
        val split = raw?.split(":")
        val resStatus = split?.get(1)
        val epochSecondsRes = split?.get(2)?.toLongOrNull()
        Log.d(TAG, "Sync end response: $resStatus")
        return when (resStatus) {
            "OK" -> SyncEndResult.Ok(raw, epochSecondsRes ?: 0L)
            else -> SyncEndResult.Error("Unknown response: $raw")
        }
    }

    open suspend fun requestFullFreshSync(epochSeconds: Long, alarms: List<AlarmEntity>) : SyncResult {
        val res = requestSyncFresh(epochSeconds)
        var toReturn: SyncResult? = when (res) {
            is SyncRequestResult.Error -> SyncResult.Error(res.response)
            is SyncRequestResult.Busy -> SyncResult.Error("Sync already in progress")
            is SyncRequestResult.ConError -> SyncResult.Error(res.response)
            is SyncRequestResult.Open -> null
        }
        if (toReturn != null) return toReturn
        var alarmIssues : List<Pair<Long, SingleSyncResult>> = emptyList()
        for (alarm in alarms) {
            //TODO: add dayOfWeek encoding later
            val daysOfWeek = "1111111"
            val res = requestSetAlarmInSync(
                alarm.id.toInt(),
                alarm.hour,
                alarm.minute,
                alarm.label ?: "",
                daysOfWeek
            )
            toReturn = when (res) {
                is SingleSyncResult.ConError -> SyncResult.Error(res.response)
                is SingleSyncResult.DuplicateTime -> SyncResult.Error("Duplicate time: ${res.response}")
                is SingleSyncResult.Error -> SyncResult.Error(res.response)
                is SingleSyncResult.NoSpace -> SyncResult.Error("No space: ${res.response}")
                is SingleSyncResult.Ok -> null
                is SingleSyncResult.Updated -> null
            }
            if (toReturn != null) {
                alarmIssues = alarmIssues + (alarm.id to res)
            }
        }
        val res2 = requestSyncEnd()
        toReturn = when (res2) {
            is SyncEndResult.ConError -> SyncResult.Error(res2.response)
            is SyncEndResult.Error -> SyncResult.Error(res2.response)
            is SyncEndResult.Ok -> SyncResult.Ok(res2.response, res2.epochSeconds)
        }
        if (alarmIssues.isNotEmpty()) {
            val sb = StringBuilder()
            sb.append("Alarm issues: ")
            for ((alarmId, res) in alarmIssues) {
                sb.append("$alarmId: $res, ")
            }
            toReturn = SyncResult.Error(sb.toString())
        }
        if (toReturn is SyncResult.Ok) {
            if (epochSeconds != toReturn.epochSeconds) {
                return SyncResult.Error("Epoch seconds mismatch")
            }
        }
        return toReturn
        // TODO: fix (partial-commit-on-failure) — SYNC:END below still runs unconditionally
        // and persists whatever alarms succeeded, but the caller gets SyncResult.Error and
        // has no way to know which ids landed on-device. Decide: abort session before
        // SYNC:END on first failure, or keep best-effort commit and report exactly which
        // alarm ids succeeded vs failed instead of a single opaque Error.
        // FOR NOW THE INFO IN THE STRING ERROR IS ENOUGH
    }

    private fun teardownGatt() {
        try {
            connectionSetupJob?.cancel() // <- kills any in-flight runConnectionSetup for the OLD gatt
            connectionSetupJob = null
            gatt?.let {
                intentionalDisconnect = true
                try {
                    it.disconnect()
                    it.close()
                } catch (e: SecurityException) {
                    Log.w(TAG, "Error during disconnect/close: ${e.message}")
                }
                gatt = null
                _connectionState.value = BleConnectionState.Disconnected
                _bleUiConnectionState.value = BleUiConnectionState.Disconnected // <- was missing
                failPendingGattOp()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error during teardown: ${e.message}")
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

    open suspend fun requestSyncCheck(): Result<String> {
        val command = "SYNC:CHECK"
        return request(command)
    }

    suspend fun getAndUpdateLastSync() {
        val res = requestSyncCheck()
        if (res.isSuccess) {
            val split = res.getOrNull()?.split(":")
            val lastSync = split?.get(2)?.toLongOrNull() ?: 0L
            _lastSyncEsp.value = lastSync
            prefs.edit().putLong("last_sync_esp", lastSync).apply()
            Log.d(TAG, "Last sync esp: $lastSync")
        }
    }

    fun closeNotifications() {
        notificationManager.cancelNotification()
    }

    sealed class SingleSyncResult(val id: Int, val response: String) {
        class Ok(id: Int, response: String) : SingleSyncResult(id, response)
        class Updated(id: Int, response: String) : SingleSyncResult(id, response)
        class DuplicateTime(id: Int, response: String) : SingleSyncResult(id, "Duplicate time: $response")
        class NoSpace(id: Int, response: String) : SingleSyncResult(id, "No space: $response")
        class Error(id: Int, response: String) : SingleSyncResult(id, "Error: $response")
        class ConError(id: Int, response: String) : SingleSyncResult(id, response)
    }

    sealed class SyncRequestResult(val response: String) {
        class Open(response: String) : SyncRequestResult(response)
        class Busy(response: String) : SyncRequestResult("Busy: $response")
        class Error(response: String) : SyncRequestResult("Error: $response")
        class ConError(response: String) : SyncRequestResult(response)
    }

    sealed class SyncEndResult(val response: String, val epochSeconds: Long = 0L) {
        class Ok(response: String, epochSeconds: Long) : SyncEndResult(response, epochSeconds)
        class Error(response: String) : SyncEndResult(response)
        class ConError(response: String) : SyncEndResult(response)
    }
    sealed class SyncResult(val response: String, val epochSeconds: Long = 0L) {
        class Ok(response: String, epochSeconds: Long) : SyncResult(response, epochSeconds)
        class Error(response: String) : SyncResult(response)
        class ConError(response: String) : SyncResult(response)
    }

    companion object {
        private const val KEY_ADDRESS = "saved_device_address"
        private const val KEY_NAME = "saved_device_name"
    }
    //TODO: add to activieties cancelNotification on ondestroy
}

fun epochSeconds(): Long {
    return System.currentTimeMillis() / 1000
}

fun localOffsetSeconds(): Long {
     return (TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000).toLong()
}

// ---- GATT status codes (most aren't in the public BluetoothGatt API, but are stable
// values from the underlying Bluetooth stack and well documented in the community) ----
private const val GATT_STATUS_LOCAL_FAILURE = -1              // sentinel: our own call never got issued / timed out
private const val GATT_STATUS_CONN_TIMEOUT = 8                // 0x08 - supervision timeout / went out of range
private const val GATT_STATUS_CONN_TERMINATE_PEER_USER = 19   // 0x13 - remote device closed the link gracefully
private const val GATT_STATUS_CONN_TERMINATE_LOCAL_HOST = 22  // 0x16 - our own disconnect() completed
private const val GATT_STATUS_CONN_FAIL_ESTABLISH = 62        // 0x3E - failed to establish connection
private const val GATT_STATUS_ERROR = 133                     // 0x85 - generic/unknown stack error, very common

private fun gattStatusName(status: Int): String = when (status) {
    GATT_STATUS_LOCAL_FAILURE -> "LOCAL_FAILURE"
    BluetoothGatt.GATT_SUCCESS -> "SUCCESS"
    GATT_STATUS_CONN_TIMEOUT -> "CONN_TIMEOUT"
    GATT_STATUS_CONN_TERMINATE_PEER_USER -> "PEER_TERMINATED"
    GATT_STATUS_CONN_TERMINATE_LOCAL_HOST -> "LOCAL_TERMINATED"
    GATT_STATUS_CONN_FAIL_ESTABLISH -> "FAILED_TO_ESTABLISH"
    GATT_STATUS_ERROR -> "GENERIC_ERROR(133)"
    else -> "UNKNOWN($status)"
}

/*

# BLE GATT Status Codes — Reference

## What each status means and how the code now handles it

| Status | Name | What it means | Handling |
|---|---|---|---|
| `-1` (local sentinel) | `LOCAL_FAILURE` | Our own call to `requestMtu`/`discoverServices`/`writeDescriptor`/`writeCharacteristic` returned `false` immediately, or the completion callback never fired within the timeout | Treated as a failed op — same branch as any other error, retried where the surrounding flow retries |
| `0` | `GATT_SUCCESS` | The operation completed normally | Happy path |
| `8` (0x08) | `CONN_TIMEOUT` | Supervision timeout — the peer stopped responding (out of range, radio died, **or went to deep sleep without an explicit disconnect**, which is the expected case for this device) | Unexpected disconnect → `scheduleReconnect()` |
| `19` (0x13) | `PEER_TERMINATED` | The peer explicitly closed the link while the connection was live
(not silence — an actual termination request) | Treated like an intentional disconnect → **no retry** |
| `22` (0x16) | `LOCAL_TERMINATED` | Our own `disconnect()` call completed | Normal, logged only |
| `62` (0x3E) | `FAILED_TO_ESTABLISH` | Connection attempt itself failed before reaching `STATE_CONNECTED` | Falls into the generic non-success branch on `STATE_CONNECTED` → retried unless intentional |
| `133` (0x85) | `GENERIC_ERROR` | Catch-all stack error, very common on real devices, often transient | Unexpected disconnect → `scheduleReconnect()` |
| anything else | `UNKNOWN($status)` | Undocumented/OEM-specific code | Same as generic error — retried |

## Why 19 vs 8 matters for this specific device

Deep sleep on the ESP32 cuts power to the radio abruptly — there's no time for a proper link-layer termination handshake,
so from the phone's side that reads as **`CONN_TIMEOUT` (8)**, not `PEER_TERMINATED` (19). That's important: it means the
"don't retry" branch (19) should essentially never fire during your device's normal sleep/wake cycle, and reconnect-on-wake
keeps working through the ordinary timeout/retry path. `PEER_TERMINATED` (19) would only show up if the firmware explicitly
calls a disconnect on its BLE stack (e.g. `NimBLEDevice::disconnect()`) while both sides are still awake and connected —
something like a deliberate "close the session" action, not sleep. If you don't currently have any such explicit-disconnect
call in firmware, this status is effectively dormant for now — but worth remembering it's there if you ever add one,
since it will silently stop the app from auto-reconnecting.

## What firmware should do

Nothing new is required. The BLE connection and its lifecycle are entirely centrally-controlled from the Android side —
the ESP32 doesn't need to know about, react to, or signal anything about GATT status codes; those are purely artifacts of
the *local* Android BLE stack describing what happened to the *local* connection object. The one thing worth being deliberate
about, if/when you touch the sleep path in firmware: if you ever add an explicit `disconnect()` call on the ESP32 side before
entering deep sleep (rather than just letting the radio go silent), that will surface as status 19 on the phone and, per the table
above, currently suppresses auto-reconnect. If that's not the behavior you want for a wake-alarm device, either don't call an explicit
disconnect before sleep (let it timeout at 8 instead, which already retries correctly), or flag it and the app-side handling can be adjusted.

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