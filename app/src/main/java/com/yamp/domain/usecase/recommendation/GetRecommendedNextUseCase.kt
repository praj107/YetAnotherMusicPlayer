package com.yamp.domain.usecase.recommendation

import com.yamp.data.repository.ListeningHistoryRepository
import com.yamp.data.repository.TrackRepository
import com.yamp.domain.model.Track
import com.yamp.player.PlaybackManager
import com.yamp.player.currentTrack
import com.yamp.recommendation.RecommendationEngine
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetRecommendedNextUseCase @Inject constructor(
    private val recommendationEngine: RecommendationEngine,
    private val trackRepository: TrackRepository,
    private val historyRepository: ListeningHistoryRepository,
    private val playbackManager: PlaybackManager
) {
    suspend operator fun invoke(limit: Int = 10): List<Track> {
        val allTracks = trackRepository.getAllTracks().first()
        val currentTrack = playbackManager.playbackState.value.currentTrack
        val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        val recentHistory = historyRepository.getRecentHistory(oneDayAgo)

        return recommendationEngine.getRecommendations(
            currentTrack = currentTrack,
            recentHistory = recentHistory,
            allTracks = allTracks,
            limit = limit
        )
    }
}
