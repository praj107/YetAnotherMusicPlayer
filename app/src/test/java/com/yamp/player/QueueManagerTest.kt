package com.yamp.player

import com.google.common.truth.Truth.assertThat
import com.yamp.domain.model.Track
import org.junit.Before
import org.junit.Test

class QueueManagerTest {

    private lateinit var queueManager: QueueManager
    private val tracks = (1..5).map { createTrack(it.toLong()) }

    @Before
    fun setup() {
        queueManager = QueueManager()
    }

    @Test
    fun `setQueue initializes queue and current index`() {
        queueManager.setQueue(tracks, startIndex = 2)
        assertThat(queueManager.currentTrack?.id).isEqualTo(3L)
        assertThat(queueManager.queue).hasSize(5)
        assertThat(queueManager.currentIndex).isEqualTo(2)
    }

    @Test
    fun `skipToNext advances index`() {
        queueManager.setQueue(tracks, 0)
        val next = queueManager.skipToNext()
        assertThat(next?.id).isEqualTo(2L)
        assertThat(queueManager.currentIndex).isEqualTo(1)
    }

    @Test
    fun `skipToNext wraps around to beginning`() {
        queueManager.setQueue(tracks, 4)
        val next = queueManager.skipToNext()
        assertThat(next?.id).isEqualTo(1L)
        assertThat(queueManager.currentIndex).isEqualTo(0)
    }

    @Test
    fun `skipToPrevious goes back`() {
        queueManager.setQueue(tracks, 2)
        val prev = queueManager.skipToPrevious()
        assertThat(prev?.id).isEqualTo(2L)
        assertThat(queueManager.currentIndex).isEqualTo(1)
    }

    @Test
    fun `skipToPrevious wraps to end from beginning`() {
        queueManager.setQueue(tracks, 0)
        val prev = queueManager.skipToPrevious()
        assertThat(prev?.id).isEqualTo(5L)
    }

    @Test
    fun `hasNext is true when not at end`() {
        queueManager.setQueue(tracks, 0)
        assertThat(queueManager.hasNext).isTrue()
    }

    @Test
    fun `hasNext is false at last position`() {
        queueManager.setQueue(tracks, 4)
        assertThat(queueManager.hasNext).isFalse()
    }

    @Test
    fun `toggleShuffle shuffles and preserves current track`() {
        queueManager.setQueue(tracks, 2)
        val currentBefore = queueManager.currentTrack
        queueManager.toggleShuffle()
        assertThat(queueManager.shuffleEnabled).isTrue()
        assertThat(queueManager.currentTrack).isEqualTo(currentBefore)
        assertThat(queueManager.currentIndex).isEqualTo(0)
    }

    @Test
    fun `toggleShuffle off restores original order`() {
        queueManager.setQueue(tracks, 2)
        queueManager.toggleShuffle() // on
        queueManager.toggleShuffle() // off
        assertThat(queueManager.shuffleEnabled).isFalse()
    }

    @Test
    fun `addToQueue adds track at end`() {
        queueManager.setQueue(tracks.take(3), 0)
        val newTrack = createTrack(99)
        queueManager.addToQueue(newTrack)
        assertThat(queueManager.queue).hasSize(4)
        assertThat(queueManager.queue.last().id).isEqualTo(99L)
    }

    @Test
    fun `clear empties queue`() {
        queueManager.setQueue(tracks, 0)
        queueManager.clear()
        assertThat(queueManager.queue).isEmpty()
        assertThat(queueManager.currentTrack).isNull()
    }

    @Test
    fun `skipToNext on empty queue returns null`() {
        assertThat(queueManager.skipToNext()).isNull()
    }

    private fun createTrack(id: Long) = Track(
        id = id, contentUri = "content://media/$id", title = "Track $id",
        artist = "Artist", album = "Album", albumArtUri = null, genre = "Rock",
        duration = 200000, trackNumber = null, year = null, mimeType = "audio/mpeg",
        folderPath = "/Music", metadataComplete = true
    )
}
