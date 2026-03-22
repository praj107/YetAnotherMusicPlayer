package com.yamp.crash

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import androidx.core.content.FileProvider
import com.yamp.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrashReporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "CrashReporter"
        private const val ISSUE_URL = "https://github.com/praj107/YetAnotherMusicPlayer/issues/new"
        private const val ISSUE_REPORT_PREVIEW_LIMIT = 1_200
    }

    private val reportsDir = File(context.filesDir, "diagnostics")
    private val pendingReportFile = File(reportsDir, "pending-crash-report.txt")
    private val prefs = context.getSharedPreferences("yamp_crash_reporter", Context.MODE_PRIVATE)
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US)
    private val _pendingReport = MutableStateFlow(loadPendingReport())
    val pendingReport: StateFlow<PendingCrashReport?> = _pendingReport.asStateFlow()

    fun install() {
        YampDiagnostics.initialize(reportsDir)
        captureHistoricalExitInfo()

        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                writePendingCrashReport(
                    title = throwable.javaClass.simpleName.ifBlank { "Fatal crash" },
                    summary = throwable.message ?: "Unhandled exception on ${thread.name}",
                    body = buildCrashBody(
                        title = "Fatal crash",
                        summary = throwable.message ?: throwable.javaClass.name,
                        stackTrace = android.util.Log.getStackTraceString(throwable)
                    )
                )
                YampDiagnostics.e(TAG, "Captured fatal crash on ${thread.name}", throwable)
            } catch (_: Exception) {
                // Avoid masking the original crash.
            } finally {
                previousHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    fun dismissPendingReport() {
        if (pendingReportFile.exists()) {
            pendingReportFile.delete()
        }
        _pendingReport.value = null
    }

    fun buildIssueIntent(report: PendingCrashReport): Intent {
        val reportPreview = readReportText(report)
            .lineSequence()
            .take(18)
            .joinToString("\n")
            .take(ISSUE_REPORT_PREVIEW_LIMIT)

        val issueBody = buildString {
            appendLine("### Crash Summary")
            appendLine("- Detected: ${formatTimestamp(report.detectedAt)}")
            appendLine("- App: YAMP ${BuildConfig.VERSION_NAME}")
            appendLine("- Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("- Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine()
            appendLine("### What Happened")
            appendLine(report.summary)
            appendLine()
            appendLine("### Diagnostics")
            appendLine("The app also copies the full diagnostics report to the clipboard when you choose Open Issue.")
            appendLine("GitHub's prefilled issue URLs support the title/body fields, but they do not support auto-attaching a local file.")
            if (reportPreview.isNotBlank()) {
                appendLine()
                appendLine("<details>")
                appendLine("<summary>Crash report preview</summary>")
                appendLine()
                appendLine("```text")
                appendLine(reportPreview)
                appendLine("```")
                appendLine("</details>")
            }
        }

        val uri = Uri.parse(ISSUE_URL).buildUpon()
            .appendQueryParameter("labels", "bug")
            .appendQueryParameter("title", "Crash: ${report.title}")
            .appendQueryParameter("body", issueBody)
            .build()

        return Intent(Intent.ACTION_VIEW, uri)
    }

    fun copyReportToClipboard(report: PendingCrashReport): Boolean {
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return false
        val reportText = readReportText(report)
        if (reportText.isBlank()) return false

        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                "YAMP crash report",
                reportText
            )
        )
        return true
    }

    fun buildShareIntent(report: PendingCrashReport): Intent? {
        val file = File(report.filePath)
        if (!file.exists()) return null

        val uri = FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "YAMP crash report ${formatTimestamp(report.detectedAt)}")
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, readReportText(report))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun captureHistoricalExitInfo() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || pendingReportFile.exists()) return

        val activityManager = context.getSystemService(ActivityManager::class.java) ?: return
        val lastSeenTimestamp = prefs.getLong("last_exit_timestamp", 0L)
        val latestExit = activityManager.getHistoricalProcessExitReasons(null, 0, 10)
            .filter { it.timestamp > lastSeenTimestamp }
            .firstOrNull { it.reason == ApplicationExitInfo.REASON_CRASH || it.reason == ApplicationExitInfo.REASON_ANR }
            ?: return

        prefs.edit().putLong("last_exit_timestamp", latestExit.timestamp).apply()

        val traceSnippet = latestExit.traceInputStream?.bufferedReader()?.use { reader ->
            reader.readText().take(8_000)
        }.orEmpty()

        writePendingCrashReport(
            title = when (latestExit.reason) {
                ApplicationExitInfo.REASON_ANR -> "ANR"
                else -> "Process crash"
            },
            summary = latestExit.description ?: "Android reported an abnormal process exit.",
            body = buildCrashBody(
                title = "Historical process exit",
                summary = latestExit.description ?: "Android reported an abnormal process exit.",
                stackTrace = traceSnippet.ifBlank { "No trace stream was available from ApplicationExitInfo." }
            ),
            detectedAt = latestExit.timestamp
        )
        YampDiagnostics.w(TAG, "Captured historical exit reason ${latestExit.reason}")
    }

    private fun buildCrashBody(
        title: String,
        summary: String,
        stackTrace: String
    ): String = buildString {
        appendLine("YAMP Crash Report")
        appendLine("Detected: ${formatTimestamp(System.currentTimeMillis())}")
        appendLine("Title: $title")
        appendLine("Summary: $summary")
        appendLine()
        appendLine("App")
        appendLine("Version: ${BuildConfig.VERSION_NAME}")
        appendLine("Package: ${BuildConfig.APPLICATION_ID}")
        appendLine()
        appendLine("Device")
        appendLine("Manufacturer: ${Build.MANUFACTURER}")
        appendLine("Model: ${Build.MODEL}")
        appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        appendLine("ABI: ${Build.SUPPORTED_ABIS.joinToString()}")
        appendLine()
        appendLine("Process")
        appendLine("PID: ${Process.myPid()}")
        appendLine("Available processors: ${Runtime.getRuntime().availableProcessors()}")
        appendLine()
        appendLine("Stack Trace / Exit Trace")
        appendLine(stackTrace.ifBlank { "No stack trace was captured." })
        appendLine()
        appendLine("Recent Diagnostics Log")
        appendLine(YampDiagnostics.readRecentLogs())
    }

    private fun writePendingCrashReport(
        title: String,
        summary: String,
        body: String,
        detectedAt: Long = System.currentTimeMillis()
    ) {
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }
        pendingReportFile.writeText(body)
        _pendingReport.value = PendingCrashReport(
            title = title,
            summary = summary,
            detectedAt = detectedAt,
            filePath = pendingReportFile.absolutePath
        )
    }

    private fun loadPendingReport(): PendingCrashReport? {
        if (!pendingReportFile.exists()) return null
        val lines = pendingReportFile.readLines()
        val title = lines.firstOrNull { it.startsWith("Title: ") }
            ?.removePrefix("Title: ")
            ?.ifBlank { "Crash report" }
            ?: "Crash report"
        val summary = lines.firstOrNull { it.startsWith("Summary: ") }
            ?.removePrefix("Summary: ")
            ?.ifBlank { "Diagnostics report available." }
            ?: "Diagnostics report available."

        return PendingCrashReport(
            title = title,
            summary = summary,
            detectedAt = pendingReportFile.lastModified(),
            filePath = pendingReportFile.absolutePath
        )
    }

    private fun formatTimestamp(timestamp: Long): String =
        timestampFormat.format(Date(timestamp))

    private fun readReportText(report: PendingCrashReport): String {
        val file = File(report.filePath)
        if (!file.exists()) return ""
        return file.readText()
    }
}
