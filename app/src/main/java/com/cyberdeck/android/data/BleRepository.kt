package com.cyberdeck.android.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import com.cyberdeck.android.core.AppPermissions
import com.cyberdeck.android.core.EventLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BleDevice(
    val address: String, val name: String, val rssi: Int, val connectable: Boolean,
    val uuids: List<String>, val manufacturer: String, val firstSeen: Long, val lastSeen: Long,
    val rssiHistory: List<Int>
)

class BleRepository {
    private val _devices = MutableStateFlow<List<BleDevice>>(emptyList())
    val devices: StateFlow<List<BleDevice>> = _devices.asStateFlow()
    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private var callback: ScanCallback? = null
    private var contextRef: Context? = null

    @SuppressLint("MissingPermission")
    fun start(context: Context) {
        contextRef = context.applicationContext
        _error.value = null
        if (!AppPermissions.granted(context, AppPermissions.blePermissions())) {
            _error.value = "Bluetooth scan/connect permission denied"; EventLog.add("BLE scan blocked: permission"); return
        }
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (adapter == null) { _error.value = "No Bluetooth adapter"; return }
        if (!adapter.isEnabled) { _error.value = "Bluetooth is disabled"; return }
        val scanner = adapter.bluetoothLeScanner ?: run { _error.value = "BLE scanner unavailable"; return }
        stop()
        val cb = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) = ingest(result)
            override fun onBatchScanResults(results: MutableList<ScanResult>) { results.forEach { ingest(it) } }
            override fun onScanFailed(errorCode: Int) { _error.value = "Scan failed code=$errorCode"; _scanning.value = false }
        }
        callback = cb
        try { scanner.startScan(cb); _scanning.value = true; EventLog.add("BLE scanner started") }
        catch (e: Exception) { _error.value = e.message; _scanning.value = false }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        val ctx = contextRef ?: return
        val scanner = (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter?.bluetoothLeScanner
        callback?.let { try { scanner?.stopScan(it) } catch (_: Exception) {} }
        callback = null
        if (_scanning.value) EventLog.add("BLE scanner stopped (${_devices.value.size} devices)")
        _scanning.value = false
    }

    @SuppressLint("MissingPermission")
    private fun ingest(result: ScanResult) {
        val rec = result.scanRecord
        val now = System.currentTimeMillis()
        val addr = result.device.address ?: "unknown"
        val name = rec?.deviceName ?: result.device.name ?: "(unnamed)"
        val uuids = rec?.serviceUuids?.map { it.toString() }.orEmpty()
        val mfg = rec?.manufacturerSpecificData?.let { sparse ->
            (0 until sparse.size()).joinToString { idx -> "id=0x${sparse.keyAt(idx).toString(16)} len=${sparse.valueAt(idx)?.size ?: 0}" }
        } ?: ""
        val connectable = if (Build.VERSION.SDK_INT >= 26) result.isConnectable else true
        val current = _devices.value.toMutableList()
        val existing = current.indexOfFirst { it.address == addr }
        if (existing >= 0) {
            val old = current[existing]
            current[existing] = old.copy(
                name = if (name != "(unnamed)") name else old.name,
                rssi = result.rssi, connectable = connectable,
                uuids = uuids.ifEmpty { old.uuids }, manufacturer = mfg.ifBlank { old.manufacturer },
                lastSeen = now, rssiHistory = (old.rssiHistory + result.rssi).takeLast(24)
            )
        } else {
            current += BleDevice(addr, name, result.rssi, connectable, uuids, mfg, now, now, listOf(result.rssi))
        }
        _devices.value = current.sortedByDescending { it.rssi }
    }
}
