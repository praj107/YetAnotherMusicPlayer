package com.yamp.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.yamp.data.local.db.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {

    @Query("SELECT * FROM tracks ORDER BY title ASC")
    fun getAllTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrackById(id: Long): TrackEntity?

    @Query("SELECT * FROM tracks")
    suspend fun getAllTracksSnapshot(): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE id IN (:ids)")
    suspend fun getTracksByIds(ids: List<Long>): List<TrackEntity>

    @Query("""
        SELECT * FROM tracks
        WHERE title LIKE '%' || :query || '%'
        OR artist LIKE '%' || :query || '%'
        OR album LIKE '%' || :query || '%'
        OR genre LIKE '%' || :query || '%'
    """)
    fun searchTracks(query: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE album = :album ORDER BY trackNumber ASC, title ASC")
    fun getTracksByAlbum(album: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE artist = :artist ORDER BY title ASC")
    fun getTracksByArtist(artist: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE genre = :genre ORDER BY title ASC")
    fun getTracksByGenre(genre: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE metadataComplete = 0")
    suspend fun getIncompleteMetadataTracks(): List<TrackEntity>

    @Query("SELECT * FROM tracks ORDER BY title ASC")
    fun getTracksSortedByTitle(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY artist ASC, title ASC")
    fun getTracksSortedByArtist(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY album ASC, trackNumber ASC")
    fun getTracksSortedByAlbum(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY genre ASC, title ASC")
    fun getTracksSortedByGenre(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY duration ASC")
    fun getTracksSortedByDuration(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY dateAdded DESC")
    fun getTracksSortedByDateAdded(): Flow<List<TrackEntity>>

    @Query("SELECT DISTINCT artist FROM tracks WHERE artist IS NOT NULL ORDER BY artist ASC")
    fun getAllArtistNames(): Flow<List<String>>

    @Query("SELECT DISTINCT album FROM tracks WHERE album IS NOT NULL ORDER BY album ASC")
    fun getAllAlbumNames(): Flow<List<String>>

    @Query("SELECT DISTINCT genre FROM tracks WHERE genre IS NOT NULL ORDER BY genre ASC")
    fun getAllGenreNames(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM tracks WHERE artist = :artist")
    suspend fun getTrackCountForArtist(artist: String): Int

    @Query("SELECT COUNT(DISTINCT album) FROM tracks WHERE artist = :artist")
    suspend fun getAlbumCountForArtist(artist: String): Int

    @Query("SELECT COUNT(*) FROM tracks WHERE genre = :genre")
    suspend fun getTrackCountForGenre(genre: String): Int

    @Upsert
    suspend fun upsertTracks(tracks: List<TrackEntity>)

    @Query("UPDATE tracks SET fileHash = :fileHash WHERE id = :trackId")
    suspend fun updateFileHash(trackId: Long, fileHash: String)

    @Query(
        """
        UPDATE tracks
        SET fileHash = COALESCE(:fileHash, fileHash),
            musicBrainzId = COALESCE(:mbid, musicBrainzId),
            title = COALESCE(:title, title),
            artist = COALESCE(:artist, artist),
            album = COALESCE(:album, album),
            albumArtUri = COALESCE(:albumArtUri, albumArtUri),
            genre = COALESCE(:genre, genre),
            year = COALESCE(:year, year),
            metadataComplete = :metadataComplete
        WHERE id = :trackId
        """
    )
    suspend fun updateMetadata(
        trackId: Long,
        fileHash: String?,
        mbid: String?,
        title: String?,
        artist: String?,
        album: String?,
        albumArtUri: String?,
        genre: String?,
        year: Int?,
        metadataComplete: Boolean
    )

    @Query("DELETE FROM tracks WHERE id NOT IN (:activeIds)")
    suspend fun deleteStale(activeIds: List<Long>)

    @Query("SELECT COUNT(*) FROM tracks")
    suspend fun getTrackCount(): Int
}
