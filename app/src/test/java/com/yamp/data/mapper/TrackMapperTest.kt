package com.yamp.data.mapper

import com.google.common.truth.Truth.assertThat
import com.yamp.data.local.db.entity.TrackEntity
import org.junit.Test

class TrackMapperTest {

    @Test
    fun `toDomain maps all fields correctly`() {
        val entity = createTrackEntity(artist = "Test Artist", album = "Test Album")
        val track = entity.toDomain()

        assertThat(track.id).isEqualTo(1L)
        assertThat(track.title).isEqualTo("Test Song")
        assertThat(track.artist).isEqualTo("Test Artist")
        assertThat(track.album).isEqualTo("Test Album")
        assertThat(track.duration).isEqualTo(240000L)
        assertThat(track.metadataComplete).isTrue()
    }

    @Test
    fun `toDomain maps null artist to Unknown Artist`() {
        val entity = createTrackEntity(artist = null)
        val track = entity.toDomain()
        assertThat(track.artist).isEqualTo("Unknown Artist")
    }

    @Test
    fun `toDomain maps null album to Unknown Album`() {
        val entity = createTrackEntity(album = null)
        val track = entity.toDomain()
        assertThat(track.album).isEqualTo("Unknown Album")
    }

    @Test
    fun `list toDomain maps all items`() {
        val entities = listOf(
            createTrackEntity(id = 1),
            createTrackEntity(id = 2),
            createTrackEntity(id = 3)
        )
        val tracks = entities.toDomain()
        assertThat(tracks).hasSize(3)
        assertThat(tracks.map { it.id }).containsExactly(1L, 2L, 3L)
    }

    private fun createTrackEntity(
        id: Long = 1L,
        artist: String? = "Artist",
        album: String? = "Album"
    ) = TrackEntity(
        id = id,
        contentUri = "content://media/external/audio/media/$id",
        title = "Test Song",
        artist = artist,
        album = album,
        albumArtUri = null,
        genre = "Rock",
        duration = 240000L,
        trackNumber = 1,
        year = 2024,
        fileSize = 5000000L,
        mimeType = "audio/mpeg",
        folderPath = "/storage/emulated/0/Music",
        dateAdded = 1700000000L,
        dateModified = 1700000000L,
        musicBrainzId = null,
        metadataComplete = true
    )
}
