package com.yamp.crash

data class PendingCrashReport(
    val title: String,
    val summary: String,
    val detectedAt: Long,
    val filePath: String
)
