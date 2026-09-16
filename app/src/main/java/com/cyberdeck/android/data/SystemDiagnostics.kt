package com.cyberdeck.android.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.WindowManager
import com.cyberdeck.android.core.formatBytes
import com.cyberdeck.android.core.formatDuration
import java.io.RandomAccessFile
import java.net.Inet4Address
import java.net.NetworkInterface

data class DashboardSnapshot(
    val cpuPercent: Float?, val ramUsed: Long, val ramTotal: Long,
    val storageUsed: Long, val storageTotal: Long,
    val batteryPercent: Int?, val batteryTempC: Float?,
    val networkType: String, val localIp: String, val dns: String,
    val deviceModel: String, val androidVersion: String, val uptime: String
)

object SystemDiagnostics {
    private var lastCpu: Pair<Long, Long>? = null

    fun snapshot(context: Context): DashboardSnapshot {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mem = ActivityManager.MemoryInfo(); am.getMemoryInfo(mem)
        val stat = StatFs(Environment.getDataDirectory().absolutePath)
        val bat = battery(context)
        return DashboardSnapshot(
            cpuUsage(), mem.totalMem - mem.availMem, mem.totalMem,
            stat.totalBytes - stat.availableBytes, stat.totalBytes,
            bat.first, bat.second, networkType(context), localIpv4(), dnsServers(context),
            "${Build.MANUFACTURER} ${Build.MODEL}",
            "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            formatDuration(SystemClock.elapsedRealtime())
        )
    }

    fun deviceReport(context: Context): String {
        val s = snapshot(context)
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION") wm.defaultDisplay.getRealMetrics(metrics)
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val pm = context.packageManager
        return buildString {
            appendLine("MODEL     ${s.deviceModel}")
            appendLine("ANDROID   ${s.androidVersion}")
            appendLine("HARDWARE  ${Build.HARDWARE}")
            appendLine("ABIS      ${Build.SUPPORTED_ABIS.joinToString()}")
            appendLine("KERNEL    ${System.getProperty("os.version")}")
            appendLine("RAM       ${formatBytes(s.ramUsed)} / ${formatBytes(s.ramTotal)}")
            appendLine("STORAGE   ${formatBytes(s.storageUsed)} / ${formatBytes(s.storageTotal)}")
            appendLine("UPTIME    ${s.uptime}")
            appendLine("SCREEN    ${metrics.widthPixels}x${metrics.heightPixels}")
            appendLine("REFRESH   ${wm.defaultDisplay.refreshRate} Hz")
            listOf("android.hardware.wifi","android.hardware.bluetooth_le","android.hardware.camera","android.hardware.location.gps").forEach {
                appendLine("  ${it.substringAfterLast('.')} = ${pm.hasSystemFeature(it)}")
            }
            sm.getSensorList(Sensor.TYPE_ALL).take(20).forEach { appendLine("  ${it.name}") }
        }
    }

    fun batteryReport(context: Context): String {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val temp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val volt = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return "PERCENT   ${(level * 100) / scale}%\nTEMP      ${temp / 10f} C\nVOLTAGE   $volt mV\nSTATUS    $status\nTECH      ${intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)}"
    }

    fun storageReport(): String {
        val st = StatFs(Environment.getDataDirectory().absolutePath)
        return "Internal ${Environment.getDataDirectory()}\n  total ${formatBytes(st.totalBytes)}\n  free  ${formatBytes(st.availableBytes)}"
    }

    private fun battery(context: Context): Pair<Int?, Float?> {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val temp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        return (if (level >= 0) (level * 100) / scale else null) to (if (temp > 0) temp / 10f else null)
    }

    private fun cpuUsage(): Float? = try {
        val line = RandomAccessFile("/proc/stat", "r").use { it.readLine() } ?: return null
        val parts = line.split(Regex("\\s+"))
        val idle = parts[4].toLong()
        val total = parts.drop(1).take(7).sumOf { it.toLong() }
        val prev = lastCpu; lastCpu = total to idle
        if (prev == null) null else {
            val dTotal = (total - prev.first).toFloat(); val dIdle = (idle - prev.second).toFloat()
            if (dTotal <= 0f) null else ((dTotal - dIdle) / dTotal) * 100f
        }
    } catch (_: Exception) { null }

    private fun networkType(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return "offline"
        val caps = cm.getNetworkCapabilities(net) ?: return "unknown"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "other"
        }
    }

    fun localIpv4(): String = try {
        NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { it is Inet4Address && !it.isLoopbackAddress }?.hostAddress ?: "n/a"
    } catch (_: Exception) { "n/a" }

    fun dnsServers(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val lp: LinkProperties = cm.getLinkProperties(cm.activeNetwork ?: return "n/a") ?: return "n/a"
        return lp.dnsServers.joinToString { it.hostAddress ?: "?" }.ifBlank { "n/a" }
    }

    fun interfacesReport(): String = buildString {
        try {
            NetworkInterface.getNetworkInterfaces()?.toList().orEmpty().forEach { ni ->
                appendLine("${ni.name} up=${ni.isUp} mtu=${ni.mtu}")
                ni.inetAddresses.toList().forEach { appendLine("  ${it.hostAddress}") }
            }
        } catch (e: Exception) { appendLine(e.message) }
    }
}
