package com.yamp.crash

import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object YampDiagnostics {
    private const val MAX_LOG_LINES = 250
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private var logFile: File? = null

    fun initialize(logDir: File) {
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        logFile = File(logDir, "yamp-diagnostics.log").apply {
            if (!exists()) {
                createNewFile()
            }
        }
        log("INFO", "Diagnostics", "Initialized diagnostics logger")
    }

    fun i(tag: String, message: String) = log("INFO", tag, message)

    fun w(tag: String, message: String, throwable: Throwable? = null) =
        log("WARN", tag, message, throwable)

    fun e(tag: String, message: String, throwable: Throwable? = null) =
        log("ERROR", tag, message, throwable)

    fun readRecentLogs(): String {
        val file = logFile ?: return "No diagnostics log available."
        if (!file.exists()) return "No diagnostics log available."
        return file.readLines().takeLast(MAX_LOG_LINES).joinToString("\n")
    }

    private fun log(level: String, tag: String, message: String, throwable: Throwable? = null) {
        val line = buildString {
            append(timestampFormat.format(Date()))
            append(" ")
            append(level)
            append("/")
            append(tag)
            append(": ")
            append(message)
            if (throwable != null) {
                append('\n')
                append(Log.getStackTraceString(throwable))
            }
        }
        logFile?.appendText("$line\n")
    }
}
