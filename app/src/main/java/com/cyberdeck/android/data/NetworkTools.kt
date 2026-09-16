package com.cyberdeck.android.data

import com.cyberdeck.android.core.EventLog
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import kotlin.system.measureTimeMillis

object NetworkTools {
    fun ping(host: String, count: Int = 4): String {
        val target = host.trim().ifEmpty { return "usage: ping <host>" }
        EventLog.add("Ping $target x$count")
        val procPing = runCatching {
            val p = ProcessBuilder("ping", "-c", count.toString(), "-W", "2", target).redirectErrorStream(true).start()
            val out = p.inputStream.bufferedReader().readText()
            "exit=${p.waitFor()}\n$out".trim()
        }.getOrElse { it.message ?: "ping binary unavailable" }
        if (procPing.contains("not found", true) || procPing.contains("error=") || procPing.startsWith("Cannot")) {
            return procPing + "\nfallback:\n" + inetReachable(target)
        }
        return procPing
    }

    private fun inetReachable(host: String): String = buildString {
        try {
            val addr = InetAddress.getByName(host)
            appendLine("resolved ${addr.hostAddress}")
            repeat(4) { i ->
                val ok = addr.isReachable(2000)
                appendLine("seq=${i + 1} reachable=$ok")
            }
        } catch (e: Exception) { appendLine(e.message) }
    }

    fun traceroute(host: String): String {
        val target = host.trim().ifEmpty { return "usage: traceroute <host>" }
        EventLog.add("Traceroute $target")
        val via = runCatching {
            listOf(listOf("traceroute", "-n", "-w", "2", "-q", "1", "-m", "15", target), listOf("tracepath", "-n", target)).firstNotNullOfOrNull { cmd ->
                val p = ProcessBuilder(cmd).redirectErrorStream(true).start()
                val out = p.inputStream.bufferedReader().readText(); p.waitFor()
                out.takeIf { it.isNotBlank() && !it.contains("not found") }
            }
        }.getOrNull()
        if (!via.isNullOrBlank()) return via
        return buildString {
            appendLine("No traceroute binary. TTL-limited ping (best-effort).")
            appendLine("Apps cannot send raw traceroute packets.")
            try { appendLine("target ${InetAddress.getByName(target).hostAddress}") } catch (e: Exception) { appendLine(e.message); return@buildString }
            for (ttl in 1..12) {
                val result = runCatching {
                    val p = ProcessBuilder("ping", "-c", "1", "-W", "2", "-t", ttl.toString(), target).redirectErrorStream(true).start()
                    p.inputStream.bufferedReader().readText().also { p.waitFor() }
                }.getOrElse { it.message ?: "fail" }
                appendLine(String.format("%2d  %s", ttl, result.lineSequence().firstOrNull()?.take(80) ?: ""))
                if (result.contains("bytes from") && !result.contains("Time to live")) break
            }
        }
    }

    fun dnsLookup(host: String): String {
        val target = host.trim().ifEmpty { return "usage: dns <host>" }
        EventLog.add("DNS lookup $target")
        return try {
            val ms = measureTimeMillis { InetAddress.getAllByName(target) }
            val addrs = InetAddress.getAllByName(target)
            buildString {
                appendLine("host     $target"); appendLine("time     ${ms}ms")
                addrs.forEach { appendLine("A/AAAA   ${it.hostAddress}") }
            }
        } catch (e: Exception) { "failed: ${e.message}" }
    }

    fun compareResolvers(host: String): String {
        val target = host.trim().ifEmpty { return "enter a domain" }
        EventLog.add("DNS compare $target")
        val system = runCatching {
            val ms = measureTimeMillis { InetAddress.getAllByName(target) }
            "SYSTEM  ${ms}ms  " + InetAddress.getAllByName(target).joinToString { it.hostAddress ?: "?" }
        }.getOrElse { "SYSTEM  fail ${it.message}" }
        val doh = listOf(
            "Cloudflare" to "https://cloudflare-dns.com/dns-query?name=$target&type=A",
            "Google" to "https://dns.google/resolve?name=$target&type=A"
        ).joinToString("\n") { (name, url) ->
            runCatching {
                val started = System.currentTimeMillis()
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    setRequestProperty("Accept", "application/dns-json"); connectTimeout = 5000; readTimeout = 5000
                }
                val body = conn.inputStream.bufferedReader().readText()
                val took = System.currentTimeMillis() - started
                val answers = Regex("\"data\"\\s*:\\s*\"([^\"]+)\"").findAll(body).map { it.groupValues[1] }.joinToString().ifBlank { body.take(120) }
                "$name  ${took}ms  $answers"
            }.getOrElse { "$name  fail ${it.message}" }
        }
        return "$system\n$doh"
    }

    fun networkStatus(context: android.content.Context): String =
        "type     ${SystemDiagnostics.snapshot(context).networkType}\nlocal    ${SystemDiagnostics.localIpv4()}\ndns      ${SystemDiagnostics.dnsServers(context)}\n\n${SystemDiagnostics.interfacesReport()}"
}
