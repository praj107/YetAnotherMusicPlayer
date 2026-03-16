package com.yamp.data.repository

import com.yamp.domain.model.PlayEvent

interface ListeningHistoryRepository {
    suspend fun recordPlayEvent(event: PlayEvent)
    suspend fun getMostPlayedTrackIds(limit: Int): List<Long>
    suspend fun getRecentHistory(since: Long): List<PlayEvent>
    suspend fun getTimeOfDayPreferences(hour: Int): List<Long>
    suspend fun getMostSkippedTrackIds(limit: Int): List<Long>
    suspend fun getCompletionRate(trackId: Long, trackDuration: Long): Float
    suspend fun getTotalEventCount(): Int
    suspend fun getUniqueTracksPlayedCount(): Int
}
