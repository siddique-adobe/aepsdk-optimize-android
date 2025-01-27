package com.adobe.marketing.optimizeapp.impl

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.FileProvider
import com.adobe.marketing.optimizeapp.models.LogEntry
import java.io.File

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

    fun clearLogs() {
        _logs.clear()
    }

    fun shareLogs(context: Context) {
        try {

            // Create a file in the cache directory
            val file = File(context.cacheDir, "sharedLogs.txt")
            file.writeText("")

            // Write each log entry to the file
            _logs.forEach { logEntry ->
                file.appendText("${logEntry.timestamp}: ${logEntry.message}\n")
            }

            // Get a URI for the file using FileProvider
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            // Create an Intent to share the file
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Grant permission to the receiving app
            }

            // Start the share Intent
            context.startActivity(Intent.createChooser(shareIntent, "Share logs via"))
        } catch (e: Exception) {
            Log.e("LogManager", "Error sharing logs", e)
        }
    }



}