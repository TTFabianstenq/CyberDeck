package com.cyberdeck.android.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(val time: String, val message: String) {
    val line: String get() = "[$time] $message"
}

object EventLog {
    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.US)

    fun init() {
        if (_entries.value.isEmpty()) add("Event log initialized")
    }

    fun add(message: String) {
        val entry = LogEntry(fmt.format(Date()), message)
        _entries.value = (_entries.value + entry).takeLast(500)
    }

    fun clear() {
        _entries.value = emptyList()
        add("Log cleared")
    }

    fun dump(): String = _entries.value.joinToString("\n") { it.line }
}
