package com.yamp.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.yamp.data.local.db.entity.ListeningHistoryEntity

data class TrackPlayCount(
    val trackId: Long,
    val count: Int
)

@Dao
interface ListeningHistoryDao {

    @Insert
    suspend fun insert(event: ListeningHistoryEntity)

    @Query("""
        SELECT trackId, COUNT(*) as count
        FROM listening_history
        WHERE completed = 1
        GROUP BY trackId
        ORDER BY count DESC
        LIMIT :limit
    """)
    suspend fun getMostPlayed(limit: Int): List<TrackPlayCount>

    @Query("SELECT * FROM listening_history WHERE timestamp > :since ORDER BY timestamp DESC")
    suspend fun getHistorySince(since: Long): List<ListeningHistoryEntity>

    @Query("""
        SELECT trackId, COUNT(*) as count
        FROM listening_history
        WHERE hourOfDay BETWEEN :startHour AND :endHour
        AND completed = 1
        GROUP BY trackId
        ORDER BY count DESC
        LIMIT :limit
    """)
    suspend fun getTopTracksForTimeOfDay(startHour: Int, endHour: Int, limit: Int): List<TrackPlayCount>

    @Query("""
        SELECT trackId, COUNT(*) as count
        FROM listening_history
        WHERE completed = 0
        GROUP BY trackId
        ORDER BY count DESC
        LIMIT :limit
    """)
    suspend fun getMostSkipped(limit: Int): List<TrackPlayCount>

    @Query("""
        SELECT AVG(durationListened * 1.0 / :trackDuration)
        FROM listening_history
        WHERE trackId = :trackId
    """)
    suspend fun getCompletionRate(trackId: Long, trackDuration: Long): Float?

    @Query("SELECT COUNT(*) FROM listening_history")
    suspend fun getTotalEventCount(): Int

    @Query("SELECT COUNT(DISTINCT trackId) FROM listening_history")
    suspend fun getUniqueTracksPlayedCount(): Int
}
