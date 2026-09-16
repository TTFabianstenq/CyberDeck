package com.cyberdeck.android.data

import android.os.Build
import android.os.Process
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ProcTools {
    private val readable = setOf(
        "/proc/version", "/proc/uptime", "/proc/meminfo", "/proc/cpuinfo",
        "/proc/stat", "/proc/loadavg", "/proc/net/dev", "/proc/net/route",
        "/proc/net/tcp", "/proc/net/udp", "/proc/self/status", "/proc/self/cmdline",
        "/proc/sys/kernel/hostname", "/proc/sys/kernel/osrelease"
    )

    fun allowedPath(path: String): Boolean {
        val n = File(path).normalize().absolutePath
        if (n in readable) return true
        if (n.startsWith("/sys/class/net/") && !n.contains("..")) return true
        return false
    }

    fun cat(path: String, lines: Int = 80): String {
        val target = path.trim()
        if (target.isEmpty()) return "usage: cat <path>"
        if (!allowedPath(target)) {
            return "refused: $target\nonly a whitelist of /proc and /sys/class/net is readable"
        }
        return runCatching {
            File(target).readText().lineSequence().take(lines).joinToString("\n")
        }.getOrElse { "cat: ${it.message}" }
    }

    fun uname(): String =
        "${System.getProperty("os.name")} ${System.getProperty("os.arch")} ${System.getProperty("os.version")} android-${Build.VERSION.RELEASE}"

    fun hostname(): String = runCatching {
        File("/proc/sys/kernel/hostname").readText().trim()
    }.getOrElse { Build.DEVICE }

    fun dateNow(): String = SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US).format(Date())

    fun envSafe(): String {
        val keys = listOf("PATH", "ANDROID_ROOT", "ANDROID_DATA", "EXTERNAL_STORAGE", "TMPDIR", "USER", "HOME", "LANG")
        return keys.joinToString("\n") { k ->
            val v = System.getenv(k) ?: System.getProperty(k.lowercase()) ?: ""
            "$k=$v"
        }
    }

    fun getprop(): String = runCatching {
        val p = ProcessBuilder("getprop").redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText()
        p.waitFor()
        out.lineSequence().filter {
            !it.contains("serial", true) && !it.contains("imei", true) && !it.contains("token", true)
        }.take(80).joinToString("\n")
    }.getOrElse { "getprop unavailable: ${it.message}" }

    fun psSelf(): String {
        val status = runCatching { File("/proc/self/status").readText() }.getOrElse { it.message ?: "" }
        return "pid=${Process.myPid()} uid=${Process.myUid()}\n" + status.lineSequence().take(20).joinToString("\n")
    }

    fun httpHead(urlRaw: String): String {
        val raw = urlRaw.trim()
        if (raw.isEmpty()) return "usage: curl -I <https-url>"
        val url = if (raw.startsWith("http://") || raw.startsWith("https://")) raw else "https://$raw"
        if (!url.startsWith("https://")) return "refused: only https"
        return runCatching {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "HEAD"
                instanceFollowRedirects = false
                connectTimeout = 5000
                readTimeout = 5000
            }
            val code = conn.responseCode
            val headers = conn.headerFields.entries.joinToString("\n") { (k, v) -> "${k ?: ""}: ${v?.joinToString()}" }
            "HTTP $code\n$headers"
        }.getOrElse { "curl: ${it.message}" }
    }

    fun whoami(): String = "uid=${Process.myUid()} pid=${Process.myPid()} package=com.cyberdeck.android"
}
