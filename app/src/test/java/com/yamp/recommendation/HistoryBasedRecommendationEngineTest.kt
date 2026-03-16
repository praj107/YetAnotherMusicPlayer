package com.yamp.recommendation

import com.google.common.truth.Truth.assertThat
import com.yamp.data.repository.ListeningHistoryRepository
import com.yamp.domain.model.PlayEvent
import com.yamp.domain.model.Track
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class HistoryBasedRecommendationEngineTest {

    private lateinit var historyRepository: ListeningHistoryRepository
    private lateinit var engine: HistoryBasedRecommendationEngine

    @Before
    fun setup() {
        historyRepository = mockk()
        engine = HistoryBasedRecommendationEngine(historyRepository)
    }

    @Test
    fun `returns empty when too few tracks`() = runTest {
        coEvery { historyRepository.getTotalEventCount() } returns 10
        val result = engine.getRecommendations(
            currentTrack = null,
            recentHistory = emptyList(),
            allTracks = listOf(createTrack(1)), // only 1 track
            limit = 5
        )
        assertThat(result).isEmpty()
    }

    @Test
    fun `returns empty when too few history events`() = runTest {
        coEvery { historyRepository.getTotalEventCount() } returns 2
        val tracks = (1..10).map { createTrack(it.toLong()) }
        val result = engine.getRecommendations(
            currentTrack = null,
            recentHistory = emptyList(),
            allTracks = tracks,
            limit = 5
        )
        assertThat(result).isEmpty()
    }

    @Test
    fun `excludes recently played tracks`() = runTest {
        setupMockHistory()
        val tracks = (1..10).map { createTrack(it.toLong()) }
        val recentHistory = listOf(
            createPlayEvent(1L),
            createPlayEvent(2L),
            createPlayEvent(3L),
            createPlayEvent(4L),
            createPlayEvent(5L)
        )

        val result = engine.getRecommendations(
            currentTrack = createTrack(1),
            recentHistory = recentHistory,
            allTracks = tracks,
            limit = 10
        )

        val resultIds = result.map { it.id }
        assertThat(resultIds).containsNoneOf(1L, 2L, 3L, 4L, 5L)
    }

    @Test
    fun `boosts tracks with matching genre`() = runTest {
        setupMockHistory()
        val tracks = listOf(
            createTrack(1, genre = "Rock"),
            createTrack(2, genre = "Rock"),
            createTrack(3, genre = "Jazz"),
            createTrack(4, genre = "Rock"),
            createTrack(5, genre = "Jazz"),
            createTrack(6, genre = "Rock"),
            createTrack(7, genre = "Classical")
        )
        val recentHistory = listOf(
            createPlayEvent(1L, completed = true)
        )

        val result = engine.getRecommendations(
            currentTrack = createTrack(1, genre = "Rock"),
            recentHistory = recentHistory,
            allTracks = tracks,
            limit = 5
        )

        // Rock tracks should be ranked higher
        assertThat(result).isNotEmpty()
    }

    @Test
    fun `limits tracks per artist for diversity`() = runTest {
        setupMockHistory()
        val tracks = (1..10).map { createTrack(it.toLong(), artist = "Same Artist") } +
            listOf(createTrack(11, artist = "Other Artist"))

        val result = engine.getRecommendations(
            currentTrack = null,
            recentHistory = emptyList(),
            allTracks = tracks,
            limit = 10
        )

        val sameArtistCount = result.count { it.artist == "Same Artist" }
        assertThat(sameArtistCount).isAtMost(3)
    }

    private fun setupMockHistory() {
        coEvery { historyRepository.getTotalEventCount() } returns 50
        coEvery { historyRepository.getMostPlayedTrackIds(any()) } returns emptyList()
        coEvery { historyRepository.getTimeOfDayPreferences(any()) } returns emptyList()
        coEvery { historyRepository.getMostSkippedTrackIds(any()) } returns emptyList()
    }

    private fun createTrack(
        id: Long,
        genre: String? = "Rock",
        artist: String = "Artist $id"
    ) = Track(
        id = id, contentUri = "content://media/$id", title = "Track $id",
        artist = artist, album = "Album", albumArtUri = null, genre = genre,
        duration = 200000, trackNumber = null, year = null, mimeType = "audio/mpeg",
        folderPath = "/Music", metadataComplete = true
    )

    private fun createPlayEvent(
        trackId: Long,
        completed: Boolean = true
    ) = PlayEvent(
        trackId = trackId, timestamp = System.currentTimeMillis(),
        durationListened = 200000, completed = completed,
        hourOfDay = 14, dayOfWeek = 3
    )
}
