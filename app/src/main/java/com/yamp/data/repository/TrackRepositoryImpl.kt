package com.yamp.data.repository

import com.yamp.data.local.db.dao.TrackDao
import com.yamp.data.mapper.toDomain
import com.yamp.domain.model.SortDirection
import com.yamp.domain.model.SortField
import com.yamp.domain.model.SortOrder
import com.yamp.domain.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TrackRepositoryImpl @Inject constructor(
    private val trackDao: TrackDao
) : TrackRepository {

    override fun getAllTracks(): Flow<List<Track>> =
        trackDao.getAllTracks().map { it.toDomain() }

    override fun searchTracks(query: String): Flow<List<Track>> =
        trackDao.searchTracks(query).map { it.toDomain() }

    override fun getTracksByAlbum(album: String): Flow<List<Track>> =
        trackDao.getTracksByAlbum(album).map { it.toDomain() }

    override fun getTracksByArtist(artist: String): Flow<List<Track>> =
        trackDao.getTracksByArtist(artist).map { it.toDomain() }

    override fun getTracksByGenre(genre: String): Flow<List<Track>> =
        trackDao.getTracksByGenre(genre).map { it.toDomain() }

    override fun getTracksSorted(sortOrder: SortOrder): Flow<List<Track>> {
        val flow = when (sortOrder.field) {
            SortField.TITLE -> trackDao.getTracksSortedByTitle()
            SortField.ARTIST -> trackDao.getTracksSortedByArtist()
            SortField.ALBUM -> trackDao.getTracksSortedByAlbum()
            SortField.GENRE -> trackDao.getTracksSortedByGenre()
            SortField.DURATION -> trackDao.getTracksSortedByDuration()
            SortField.DATE_ADDED -> trackDao.getTracksSortedByDateAdded()
        }
        return flow.map { entities ->
            val tracks = entities.toDomain()
            if (sortOrder.direction == SortDirection.DESCENDING) tracks.reversed() else tracks
        }
    }

    override suspend fun getTrackById(id: Long): Track? =
        trackDao.getTrackById(id)?.toDomain()

    override suspend fun getTrackCount(): Int =
        trackDao.getTrackCount()
}
