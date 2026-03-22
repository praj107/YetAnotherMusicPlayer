package com.yamp.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.yamp.crash.PendingCrashReport

@Composable
fun CrashReportDialog(
    report: PendingCrashReport,
    onOpenIssue: () -> Unit,
    onShareReport: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Crash report ready",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = report.summary,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onOpenIssue) {
                Text("Open Issue")
            }
        },
        dismissButton = {
            TextButton(onClick = onShareReport) {
                Text("Share Report")
            }
        }
    )
}
