package com.cyberdeck.android.core

import java.util.Locale

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var v = bytes.toDouble()
    var i = -1
    do {
        v /= 1024.0
        i++
    } while (v >= 1024 && i < units.lastIndex)
    return String.format(Locale.US, "%.2f %s", v, units[i])
}

fun formatDuration(ms: Long): String {
    val s = ms / 1000
    val d = s / 86400
    val h = (s % 86400) / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (d > 0) "${d}d ${h}h ${m}m ${sec}s" else "${h}h ${m}m ${sec}s"
}
