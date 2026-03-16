package com.yamp.recommendation

import com.yamp.data.repository.ListeningHistoryRepository
import com.yamp.domain.model.PlayEvent
import com.yamp.domain.model.Track
import java.util.Calendar
import javax.inject.Inject

class HistoryBasedRecommendationEngine @Inject constructor(
    private val historyRepository: ListeningHistoryRepository
) : RecommendationEngine {

    private val weights = ScoringWeights()
    private val minTracksForRecommendation = 5
    private val recentTrackExcludeCount = 5

    override suspend fun getRecommendations(
        currentTrack: Track?,
        recentHistory: List<PlayEvent>,
        allTracks: List<Track>,
        limit: Int
    ): List<Track> {
        if (allTracks.size < minTracksForRecommendation) return emptyList()

        val totalEvents = historyRepository.getTotalEventCount()
        if (totalEvents < 3) return emptyList()

        val recentTrackIds = recentHistory
            .take(recentTrackExcludeCount)
            .map { it.trackId }
            .toSet()

        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val mostPlayedIds = historyRepository.getMostPlayedTrackIds(50).toSet()
        val timePreferredIds = historyRepository.getTimeOfDayPreferences(currentHour).toSet()
        val skippedIds = historyRepository.getMostSkippedTrackIds(20).toSet()

        val genreAffinities = computeGenreAffinities(recentHistory, allTracks)

        val candidates = allTracks.filter { it.id !in recentTrackIds }

        val scored = candidates.map { track ->
            val score = computeScore(
                track = track,
                mostPlayedIds = mostPlayedIds,
                timePreferredIds = timePreferredIds,
                skippedIds = skippedIds,
                genreAffinities = genreAffinities,
                currentTrack = currentTrack
            )
            track to score
        }

        return scored
            .sortedByDescending { it.second }
            .map { it.first }
            .distinctByArtistLimited()
            .take(limit)
    }

    private fun computeScore(
        track: Track,
        mostPlayedIds: Set<Long>,
        timePreferredIds: Set<Long>,
        skippedIds: Set<Long>,
        genreAffinities: Map<String, Float>,
        currentTrack: Track?
    ): Float {
        var score = 0f

        // Play count factor
        if (track.id in mostPlayedIds) {
            score += weights.playCount
        }

        // Penalty for frequently skipped
        if (track.id in skippedIds) {
            score -= weights.completionRate * 0.5f
        }

        // Time of day preference
        if (track.id in timePreferredIds) {
            score += weights.timeOfDayMatch
        }

        // Genre affinity
        val trackGenre = track.genre
        if (trackGenre != null && trackGenre in genreAffinities) {
            score += weights.genreAffinity * (genreAffinities[trackGenre] ?: 0f)
        }

        // Boost for same genre as current track
        if (currentTrack != null && track.genre != null && track.genre == currentTrack.genre) {
            score += weights.genreAffinity * 0.5f
        }

        // Boost for same artist as current track (small)
        if (currentTrack != null && track.artist == currentTrack.artist) {
            score += 0.1f
        }

        return score
    }

    private fun computeGenreAffinities(
        recentHistory: List<PlayEvent>,
        allTracks: List<Track>
    ): Map<String, Float> {
        val trackMap = allTracks.associateBy { it.id }
        val genreCounts = mutableMapOf<String, Int>()
        var total = 0

        for (event in recentHistory) {
            if (event.completed) {
                val track = trackMap[event.trackId]
                val genre = track?.genre ?: continue
                genreCounts[genre] = (genreCounts[genre] ?: 0) + 1
                total++
            }
        }

        if (total == 0) return emptyMap()
        return genreCounts.mapValues { (_, count) -> count.toFloat() / total }
    }

    private fun List<Track>.distinctByArtistLimited(maxPerArtist: Int = 3): List<Track> {
        val artistCount = mutableMapOf<String, Int>()
        return filter { track ->
            val count = artistCount.getOrDefault(track.artist, 0)
            if (count < maxPerArtist) {
                artistCount[track.artist] = count + 1
                true
            } else false
        }
    }
}
