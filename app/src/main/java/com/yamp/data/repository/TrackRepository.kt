package com.yamp.data.repository

import com.yamp.domain.model.SortOrder
import com.yamp.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface TrackRepository {
    fun getAllTracks(): Flow<List<Track>>
    fun searchTracks(query: String): Flow<List<Track>>
    fun getTracksByAlbum(album: String): Flow<List<Track>>
    fun getTracksByArtist(artist: String): Flow<List<Track>>
    fun getTracksByGenre(genre: String): Flow<List<Track>>
    fun getTracksSorted(sortOrder: SortOrder): Flow<List<Track>>
    suspend fun getTrackById(id: Long): Track?
    suspend fun getTrackCount(): Int
}
