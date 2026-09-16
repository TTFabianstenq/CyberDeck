package com.cyberdeck.android.data

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import com.cyberdeck.android.core.AppPermissions
import com.cyberdeck.android.core.EventLog

data class WifiAp(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequency: Int,
    val channel: Int,
    val security: String,
    val widthMhz: Int
)

object WifiRepository {
    fun channelFromHz(freq: Int): Int = when {
        freq in 2412..2484 -> (freq - 2412) / 5 + 1
        freq in 5000..5900 -> (freq - 5000) / 5
        freq in 5955..7115 -> (freq - 5955) / 5 + 1
        else -> -1
    }

    fun securityOf(result: ScanResult): String {
        val caps = result.capabilities ?: ""
        return when {
            caps.contains("WPA3") -> "WPA3"
            caps.contains("WPA2") -> "WPA2"
            caps.contains("WPA") -> "WPA"
            caps.contains("WEP") -> "WEP"
            caps.contains("SAE") -> "SAE/WPA3"
            caps.contains("EAP") -> "EAP"
            caps.contains("OWE") -> "OWE"
            caps.contains("ESS") && !caps.contains("WPA") && !caps.contains("WEP") -> "Open"
            else -> caps.ifBlank { "unknown" }
        }
    }

    @SuppressLint("MissingPermission")
    fun scan(context: Context): Result<List<WifiAp>> {
        if (!AppPermissions.granted(context, AppPermissions.wifiScanPermissions())) {
            return Result.failure(IllegalStateException("Wi-Fi scan needs location and/or NEARBY_WIFI_DEVICES"))
        }
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (!wm.isWifiEnabled) return Result.failure(IllegalStateException("Wi-Fi is disabled"))
        @Suppress("DEPRECATION")
        val started = wm.startScan()
        EventLog.add("Wi-Fi scan requested started=$started")
        val results = try { wm.scanResults } catch (e: SecurityException) { return Result.failure(e) }
        val mapped = results.map { r ->
            val width = if (Build.VERSION.SDK_INT >= 23) when (r.channelWidth) {
                ScanResult.CHANNEL_WIDTH_20MHZ -> 20
                ScanResult.CHANNEL_WIDTH_40MHZ -> 40
                ScanResult.CHANNEL_WIDTH_80MHZ -> 80
                ScanResult.CHANNEL_WIDTH_160MHZ -> 160
                else -> 20
            } else 20
            val ssid = if (Build.VERSION.SDK_INT >= 33) r.wifiSsid?.toString()?.trim('"') ?: r.SSID else r.SSID
            WifiAp(ssid.ifBlank { "<hidden>" }, r.BSSID ?: "?", r.level, r.frequency, channelFromHz(r.frequency), securityOf(r), width)
        }.sortedByDescending { it.rssi }
        EventLog.add("Wi-Fi scan results=${mapped.size}")
        return Result.success(mapped)
    }
}
