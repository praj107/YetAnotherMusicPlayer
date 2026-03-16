package com.yamp.recommendation

import com.yamp.domain.model.PlayEvent
import com.yamp.domain.model.Track

interface RecommendationEngine {
    suspend fun getRecommendations(
        currentTrack: Track?,
        recentHistory: List<PlayEvent>,
        allTracks: List<Track>,
        limit: Int = 10
    ): List<Track>
}
