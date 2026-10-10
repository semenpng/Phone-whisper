package com.example.monitor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel(val label: String) {
    INFO("INFO"),
    PERF("PERF"),
    DEBUG("DEBUG"),
    WARN("WARN"),
    ERROR("ERROR")
}

data class LogEntry(
    val id: Long = System.nanoTime(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val tag: String,
    val message: String
) {
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    fun formatted(): String {
        val timeStr = timeFormat.format(Date(timestamp))
        return "[$timeStr] [${level.label}] [$tag] $message"
    }
}

object ExecutionLogger {
    private const val MAX_LOGS = 600
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun log(level: LogLevel, tag: String, message: String) {
        val entry = LogEntry(level = level, tag = tag, message = message)
        val current = _logs.value.toMutableList()
        current.add(entry)
        if (current.size > MAX_LOGS) {
            current.removeAt(0)
        }
        _logs.value = current
    }

    fun i(tag: String, message: String) = log(LogLevel.INFO, tag, message)
    fun p(tag: String, message: String) = log(LogLevel.PERF, tag, message)
    fun d(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)
    fun w(tag: String, message: String) = log(LogLevel.WARN, tag, message)
    fun e(tag: String, message: String) = log(LogLevel.ERROR, tag, message)

    fun clear() {
        _logs.value = emptyList()
    }

    fun getAllFormattedText(): String {
        return _logs.value.joinToString("\n") { it.formatted() }
    }
}
