package com.yamp.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.yamp.data.local.db.dao.TrackDao
import com.yamp.data.local.db.entity.TrackEntity
import com.yamp.domain.model.SortDirection
import com.yamp.domain.model.SortField
import com.yamp.domain.model.SortOrder
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class TrackRepositoryImplTest {

    private lateinit var trackDao: TrackDao
    private lateinit var repository: TrackRepositoryImpl

    @Before
    fun setup() {
        trackDao = mockk()
        repository = TrackRepositoryImpl(trackDao)
    }

    @Test
    fun `getAllTracks returns mapped domain objects`() = runTest {
        val entities = listOf(createEntity(1), createEntity(2))
        every { trackDao.getAllTracks() } returns flowOf(entities)

        repository.getAllTracks().test {
            val tracks = awaitItem()
            assertThat(tracks).hasSize(2)
            assertThat(tracks[0].id).isEqualTo(1L)
            assertThat(tracks[1].id).isEqualTo(2L)
            awaitComplete()
        }
    }

    @Test
    fun `searchTracks delegates to dao`() = runTest {
        val entities = listOf(createEntity(1, title = "Hello World"))
        every { trackDao.searchTracks("Hello") } returns flowOf(entities)

        repository.searchTracks("Hello").test {
            val tracks = awaitItem()
            assertThat(tracks).hasSize(1)
            assertThat(tracks[0].title).isEqualTo("Hello World")
            awaitComplete()
        }
    }

    @Test
    fun `getTracksSorted with descending reverses list`() = runTest {
        val entities = listOf(createEntity(1, title = "A"), createEntity(2, title = "B"))
        every { trackDao.getTracksSortedByTitle() } returns flowOf(entities)

        repository.getTracksSorted(SortOrder(SortField.TITLE, SortDirection.DESCENDING)).test {
            val tracks = awaitItem()
            assertThat(tracks[0].title).isEqualTo("B")
            assertThat(tracks[1].title).isEqualTo("A")
            awaitComplete()
        }
    }

    @Test
    fun `getTrackById returns null for missing track`() = runTest {
        coEvery { trackDao.getTrackById(999) } returns null
        val result = repository.getTrackById(999)
        assertThat(result).isNull()
    }

    private fun createEntity(id: Long, title: String = "Track $id") = TrackEntity(
        id = id, contentUri = "content://media/$id", title = title,
        artist = "Artist", album = "Album", albumArtUri = null, genre = "Rock",
        duration = 200000, trackNumber = null, year = null, fileSize = 1000,
        mimeType = "audio/mpeg", folderPath = "/Music", dateAdded = 0, dateModified = 0,
        musicBrainzId = null, metadataComplete = true
    )
}
