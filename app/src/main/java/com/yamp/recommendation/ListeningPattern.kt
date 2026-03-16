package com.yamp.recommendation

data class ListeningPattern(
    val trackId: Long,
    val playCount: Int,
    val skipCount: Int,
    val avgCompletionRate: Float,
    val preferredHours: Set<Int>,
    val preferredDays: Set<Int>,
    val lastPlayedTimestamp: Long
)
