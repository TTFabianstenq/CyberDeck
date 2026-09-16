package com.cyberdeck.android.data

import android.bluetooth.BluetoothManager
import android.content.Context
import com.cyberdeck.android.BuildConfig
import com.cyberdeck.android.core.EventLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TerminalEngine(private val context: Context) {
    private val history = mutableListOf<String>()

    suspend fun execute(raw: String): String = withContext(Dispatchers.IO) {
        val line = raw.trim()
        if (line.isEmpty()) return@withContext ""
        history.add(line)
        if (history.size > 100) history.removeAt(0)
        val parts = tokenize(line)
        val cmd = parts.first().lowercase()
        val args = parts.drop(1)
        EventLog.add("term $cmd")
        when (cmd) {
            "help", "man" -> HELP
            "clear", "reset" -> "__CLEAR__"
            "echo" -> args.joinToString(" ")
            "date" -> ProcTools.dateNow()
            "whoami", "id" -> ProcTools.whoami()
            "hostname" -> ProcTools.hostname()
            "uname" -> ProcTools.uname()
            "env", "printenv" -> ProcTools.envSafe()
            "getprop" -> ProcTools.getprop()
            "pwd" -> context.filesDir.absolutePath
            "ls" -> ls(args.firstOrNull() ?: context.filesDir.absolutePath)
            "cat" -> ProcTools.cat(args.firstOrNull() ?: "")
            "head" -> ProcTools.cat(args.firstOrNull() ?: "", 10)
            "free" -> ProcTools.cat("/proc/meminfo", 8)
            "df" -> SystemDiagnostics.storageReport()
            "uptime" -> SystemDiagnostics.snapshot(context).uptime + "\n" + ProcTools.cat("/proc/uptime", 1)
            "ps" -> ProcTools.psSelf()
            "ifconfig", "ip" -> SystemDiagnostics.interfacesReport()
            "netstat" -> ProcTools.cat("/proc/net/tcp", 16)
            "history" -> history.mapIndexed { i, s -> "${i + 1}  $s" }.joinToString("\n")
            "curl" -> {
                if (args.firstOrNull() == "-I" || args.firstOrNull() == "--head") ProcTools.httpHead(args.drop(1).joinToString(" "))
                else "only HEAD is implemented: curl -I https://example.com"
            }
            "device" -> SystemDiagnostics.deviceReport(context)
            "network" -> NetworkTools.networkStatus(context)
            "wifi" -> WifiRepository.scan(context).fold(
                { list -> if (list.isEmpty()) "no scan results" else list.joinToString("\n") { "${it.rssi}dBm ch${it.channel} ${it.ssid} ${it.bssid} ${it.security}" } },
                { it.message ?: "fail" }
            )
            "bluetooth" -> "adapter=${(context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter != null}"
            "dns" -> NetworkTools.dnsLookup(args.joinToString(" "))
            "ping" -> NetworkTools.ping(args.firstOrNull() ?: "")
            "traceroute", "tracepath" -> NetworkTools.traceroute(args.firstOrNull() ?: "")
            "ports" -> ProcTools.cat("/proc/net/tcp", 16)
            "apps" -> {
                val apps = AppInspector.listApps(context)
                "visible packages: ${apps.size}\n" + apps.take(40).joinToString("\n") { "${it.packageName}  ${it.versionName}" }
            }
            "permissions" -> AppInspector.listApps(context).find { it.packageName.contains("cyberdeck") }
                ?.let { AppInspector.describe(it) + "\n" + it.requested.joinToString("\n") }
                ?: "self package not visible"
            "storage" -> SystemDiagnostics.storageReport()
            "battery" -> SystemDiagnostics.batteryReport(context)
            "hash" -> hashCmd(args)
            "encode" -> if (args.size < 2) "usage: encode <base64|url|hex|binary|utf8> <text>" else CryptoTools.encode(args[0].lowercase(), args.drop(1).joinToString(" "))
            "decode" -> if (args.size < 2) "usage: decode <mode> <text>" else CryptoTools.decode(args[0].lowercase(), args.drop(1).joinToString(" "))
            "about" -> "CyberDeck ${BuildConfig.VERSION_NAME}\n${BuildConfig.APPLICATION_ID}\nlocal diagnostics only"
            "exit" -> "this is not a login shell. use Home to leave."
            else -> "$cmd: command not found\ntype help"
        }
    }

    private fun tokenize(line: String): List<String> {
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        var q = false
        for (c in line) {
            when {
                c == '"' -> q = !q
                c == ' ' && !q -> if (cur.isNotEmpty()) { out += cur.toString(); cur.clear() }
                else -> cur.append(c)
            }
        }
        if (cur.isNotEmpty()) out += cur.toString()
        return out.ifEmpty { listOf("") }
    }

    private fun ls(path: String): String {
        val f = java.io.File(path)
        if (!f.exists()) return "ls: $path: no such file"
        if (!f.canRead()) return "ls: $path: permission denied"
        if (f.isFile) return f.name + "  " + f.length() + "B"
        val kids = f.listFiles() ?: return "ls: cannot list"
        return kids.sortedBy { it.name }.joinToString("\n") {
            val mark = if (it.isDirectory) "/" else ""
            "${it.name}$mark"
        }
    }

    private fun hashCmd(args: List<String>): String {
        if (args.size < 2) return "usage: hash <md5|sha-1|sha-256|sha-512> <text>"
        val algo = when (args[0].lowercase()) {
            "md5" -> "MD5"
            "sha1", "sha-1" -> "SHA-1"
            "sha256", "sha-256" -> "SHA-256"
            "sha512", "sha-512" -> "SHA-512"
            else -> return "unsupported algorithm"
        }
        return "$algo ${CryptoTools.hashText(algo, args.drop(1).joinToString(" "))}"
    }

    companion object {
        val HELP = """
            GNU-like builtins (device-local):
              help man clear reset echo date whoami id hostname uname
              env printenv getprop pwd ls cat head free df uptime ps
              ifconfig ip netstat history curl -I
            deck tools:
              device network wifi bluetooth dns ping traceroute ports
              apps permissions storage battery hash encode decode about
            cat whitelist: /proc/{version,uptime,meminfo,cpuinfo,stat,loadavg,net/*} /sys/class/net/*
        """.trimIndent()
    }
}
