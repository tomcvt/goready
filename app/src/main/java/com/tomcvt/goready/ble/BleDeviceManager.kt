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
import kotlinx.coroutines.flow.StateFlow
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

// ---- Permission helper (min SDK 24 branch) ----
fun hasBlePermissions(context: Context): Boolean = if (Build.VERSION.SDK_INT >= 31) {
    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PERMISSION_GRANTED
} else {
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PERMISSION_GRANTED
}

// ---- The manager ----
class BleDeviceManager(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter
) {
    private val prefs = context.getSharedPreferences("ble_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // --- Public state ---
    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    val connectionState: StateFlow<BleConnectionState> = _connectionState

    private val _savedDevice = MutableStateFlow(loadSavedDevice())
    val savedDevice: StateFlow<SavedDevice?> = _savedDevice

    private val _currentDevice = MutableStateFlow<BluetoothDevice?>(null)
    val currentDevice: StateFlow<BluetoothDevice?> = _currentDevice


    private val _scanResults = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val scanResults: StateFlow<List<DiscoveredDevice>> = _scanResults

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _events = MutableSharedFlow<BleEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<BleEvent> = _events

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

    @SuppressLint("MissingPermission")
    fun tryAutoConnect() {
        if (_connectionState.value !is BleConnectionState.Disconnected) return
        val saved = _savedDevice.value ?: return
        if (!hasBlePermissions(context)) return
        runCatching { bluetoothAdapter.getRemoteDevice(saved.address) }.getOrNull()?.let {
            intentionalDisconnect = false
            retryCount = 0
            connect(it, autoConnect = false)
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
            scope.launch { delay(2000L * retryCount); connect(device, autoConnect = false) }
        } else {
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
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    val wasIntentional = intentionalDisconnect
                    _connectionState.value = BleConnectionState.Disconnected
                    failAllPendingRpc(IOException("BLE disconnected"))
                    _events.tryEmit(BleEvent.Disconnected(unexpected = !wasIntentional))
                    g.close()
                    if (!wasIntentional) scheduleReconnect(g.device)
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
        return try {
            Log.d(TAG, "Sending RPC $id: $command")
            withTimeout(timeoutMs) {
                sendCommand("$id:$command")
                Result.success(deferred.await())
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "RPC $id timed out")
            Result.failure(e)
        } finally {
            pendingRpc.remove(id)
        }
    }

    companion object {
        private const val KEY_ADDRESS = "saved_device_address"
        private const val KEY_NAME = "saved_device_name"
    }
}