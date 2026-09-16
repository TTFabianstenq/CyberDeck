package com.cyberdeck.android.data

import android.bluetooth.BluetoothManager
import android.content.Context
import com.cyberdeck.android.BuildConfig
import com.cyberdeck.android.core.EventLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TerminalEngine(private val context: Context) {
    suspend fun execute(raw: String): String = withContext(Dispatchers.IO) {
        val line = raw.trim(); if (line.isEmpty()) return@withContext ""
        val parts = line.split(Regex("\\s+")); val cmd = parts[0].lowercase(); val args = parts.drop(1)
        EventLog.add("term $cmd")
        when (cmd) {
            "help" -> HELP
            "clear" -> "__CLEAR__"
            "device" -> SystemDiagnostics.deviceReport(context)
            "network" -> NetworkTools.networkStatus(context)
            "wifi" -> WifiRepository.scan(context).fold({ list -> if (list.isEmpty()) "no scan results" else list.joinToString("\n") { "${it.rssi}dBm ch${it.channel} ${it.ssid} ${it.bssid} ${it.security}" } }, { it.message ?: "fail" })
            "bluetooth" -> "adapter=${(context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter != null}\nUse the BLE tab for live scan."
            "dns" -> NetworkTools.dnsLookup(args.joinToString(" "))
            "ping" -> NetworkTools.ping(args.firstOrNull() ?: "")
            "traceroute" -> NetworkTools.traceroute(args.firstOrNull() ?: "")
            "ports" -> localSockets()
            "apps" -> {
                val apps = AppInspector.listApps(context)
                "visible packages: ${apps.size}\n" + apps.take(40).joinToString("\n") { "${it.packageName}  ${it.versionName}" }
            }
            "permissions" -> AppInspector.listApps(context).find { it.packageName.contains("cyberdeck") }?.let { AppInspector.describe(it) + "\n" + it.requested.joinToString("\n") } ?: "self package not visible"
            "storage" -> SystemDiagnostics.storageReport()
            "battery" -> SystemDiagnostics.batteryReport(context)
            "hash" -> hashCmd(args)
            "encode" -> if (args.size < 2) "usage: encode <base64|url|hex|binary|utf8> <text>" else CryptoTools.encode(args[0].lowercase(), args.drop(1).joinToString(" "))
            "decode" -> if (args.size < 2) "usage: decode <mode> <text>" else CryptoTools.decode(args[0].lowercase(), args.drop(1).joinToString(" "))
            "uptime" -> SystemDiagnostics.snapshot(context).uptime
            "about" -> "CyberDeck ${BuildConfig.VERSION_NAME}\nLocal diagnostics. No telemetry.\n${BuildConfig.APPLICATION_ID}"
            else -> "unknown command: $cmd\ntype help"
        }
    }

    private fun hashCmd(args: List<String>): String {
        if (args.size < 2) return "usage: hash <md5|sha-1|sha-256|sha-512> <text>"
        val algo = when (args[0].lowercase()) {
            "md5" -> "MD5"; "sha1", "sha-1" -> "SHA-1"; "sha256", "sha-256" -> "SHA-256"; "sha512", "sha-512" -> "SHA-512"
            else -> return "unsupported algorithm"
        }
        return "$algo ${CryptoTools.hashText(algo, args.drop(1).joinToString(" "))}"
    }

    private fun localSockets(): String = buildString {
        appendLine("Local /proc/net tables (not a remote port scan).")
        listOf("/proc/net/tcp", "/proc/net/udp").forEach { path ->
            appendLine("== $path ==")
            appendLine(runCatching { java.io.File(path).readText().lineSequence().take(12).joinToString("\n") }.getOrElse { it.message ?: "" })
        }
    }

    companion object {
        val HELP = "commands: help clear device network wifi bluetooth dns ping traceroute ports apps permissions storage battery hash encode decode uptime about"
    }
}
