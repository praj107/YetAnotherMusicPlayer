package com.yamp.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "listening_history")
data class ListeningHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: Long,
    val timestamp: Long,
    val durationListened: Long,
    val completed: Boolean,
    val hourOfDay: Int,
    val dayOfWeek: Int
)
