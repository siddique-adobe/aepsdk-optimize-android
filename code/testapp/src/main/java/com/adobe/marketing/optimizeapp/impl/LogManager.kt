package com.adobe.marketing.optimizeapp.impl

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.adobe.marketing.optimizeapp.models.LogEntry

class LogManager {

    val showLogs = mutableStateOf(true)

    private val _logs = mutableStateListOf<LogEntry>()
    val logs: List<LogEntry> get() = _logs

    fun addLog(log: String) {
        _logs.add(
            LogEntry(
                System.currentTimeMillis().toString(),
                log
            )
        )
    }
}