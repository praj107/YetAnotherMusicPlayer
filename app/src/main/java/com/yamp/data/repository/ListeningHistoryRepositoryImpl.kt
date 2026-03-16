package com.yamp.data.repository

import com.yamp.data.local.db.dao.ListeningHistoryDao
import com.yamp.data.local.db.entity.ListeningHistoryEntity
import com.yamp.domain.model.PlayEvent
import javax.inject.Inject

class ListeningHistoryRepositoryImpl @Inject constructor(
    private val dao: ListeningHistoryDao
) : ListeningHistoryRepository {

    override suspend fun recordPlayEvent(event: PlayEvent) {
        dao.insert(
            ListeningHistoryEntity(
                trackId = event.trackId,
                timestamp = event.timestamp,
                durationListened = event.durationListened,
                completed = event.completed,
                hourOfDay = event.hourOfDay,
                dayOfWeek = event.dayOfWeek
            )
        )
    }

    override suspend fun getMostPlayedTrackIds(limit: Int): List<Long> =
        dao.getMostPlayed(limit).map { it.trackId }

    override suspend fun getRecentHistory(since: Long): List<PlayEvent> =
        dao.getHistorySince(since).map { entity ->
            PlayEvent(
                trackId = entity.trackId,
                timestamp = entity.timestamp,
                durationListened = entity.durationListened,
                completed = entity.completed,
                hourOfDay = entity.hourOfDay,
                dayOfWeek = entity.dayOfWeek
            )
        }

    override suspend fun getTimeOfDayPreferences(hour: Int): List<Long> {
        val startHour = (hour - 1).coerceAtLeast(0)
        val endHour = (hour + 1).coerceAtMost(23)
        return dao.getTopTracksForTimeOfDay(startHour, endHour, 20).map { it.trackId }
    }

    override suspend fun getMostSkippedTrackIds(limit: Int): List<Long> =
        dao.getMostSkipped(limit).map { it.trackId }

    override suspend fun getCompletionRate(trackId: Long, trackDuration: Long): Float =
        dao.getCompletionRate(trackId, trackDuration) ?: 0f

    override suspend fun getTotalEventCount(): Int =
        dao.getTotalEventCount()

    override suspend fun getUniqueTracksPlayedCount(): Int =
        dao.getUniqueTracksPlayedCount()
}
