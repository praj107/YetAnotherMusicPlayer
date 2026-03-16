package com.yamp.domain.model

data class PlayEvent(
    val trackId: Long,
    val timestamp: Long,
    val durationListened: Long,
    val completed: Boolean,
    val hourOfDay: Int,
    val dayOfWeek: Int
)
