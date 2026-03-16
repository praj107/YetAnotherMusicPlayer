package com.yamp.data.repository

import android.util.Log
import com.yamp.data.local.db.dao.MetadataCacheDao
import com.yamp.data.local.db.dao.TrackDao
import com.yamp.data.mapper.toDomain
import com.yamp.data.mapper.toMetadataCache
import com.yamp.data.remote.musicbrainz.MusicBrainzApi
import com.yamp.data.remote.musicbrainz.MusicBrainzRateLimiter
import com.yamp.domain.model.Track
import javax.inject.Inject

class MetadataRepositoryImpl @Inject constructor(
    private val musicBrainzApi: MusicBrainzApi,
    private val metadataCacheDao: MetadataCacheDao,
    private val trackDao: TrackDao,
    private val rateLimiter: MusicBrainzRateLimiter
) : MetadataRepository {

    companion object {
        private const val TAG = "MetadataRepository"
        private const val MIN_CONFIDENCE = 0.75f
    }

    override suspend fun fetchAndCacheMetadata(track: Track): Track? {
        val cached = metadataCacheDao.getCachedMetadata(track.id)
        if (cached != null && cached.confidence >= MIN_CONFIDENCE) {
            return track.copy(
                artist = cached.resolvedArtist ?: track.artist,
                album = cached.resolvedAlbum ?: track.album,
                genre = cached.resolvedGenre ?: track.genre,
                year = cached.resolvedYear ?: track.year,
                metadataComplete = true
            )
        }

        return try {
            val query = buildSearchQuery(track)
            val response = rateLimiter.throttle {
                musicBrainzApi.searchRecordings(query)
            }

            val bestMatch = response.recordings
                ?.filter { it.score >= (MIN_CONFIDENCE * 100).toInt() }
                ?.maxByOrNull { it.score }

            if (bestMatch != null) {
                val cacheEntity = bestMatch.toMetadataCache(track.id)
                metadataCacheDao.insertOrUpdate(cacheEntity)

                trackDao.updateMetadata(
                    trackId = track.id,
                    mbid = bestMatch.id,
                    artist = cacheEntity.resolvedArtist,
                    album = cacheEntity.resolvedAlbum,
                    genre = cacheEntity.resolvedGenre,
                    year = cacheEntity.resolvedYear
                )

                track.copy(
                    artist = cacheEntity.resolvedArtist ?: track.artist,
                    album = cacheEntity.resolvedAlbum ?: track.album,
                    genre = cacheEntity.resolvedGenre ?: track.genre,
                    year = cacheEntity.resolvedYear ?: track.year,
                    metadataComplete = true
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch metadata for ${track.title}", e)
            null
        }
    }

    override suspend fun getIncompleteMetadataTracks(): List<Track> =
        trackDao.getIncompleteMetadataTracks().toDomain()

    override suspend fun batchFetchMetadata(tracks: List<Track>, onProgress: (Int, Int) -> Unit) {
        tracks.forEachIndexed { index, track ->
            fetchAndCacheMetadata(track)
            onProgress(index + 1, tracks.size)
        }
    }

    private fun buildSearchQuery(track: Track): String {
        val parts = mutableListOf<String>()
        val cleanTitle = track.title
            .replace(Regex("\\.(mp3|flac|ogg|wav|m4a|aac|opus|wma)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("[_\\-]+"), " ")
            .trim()
        parts.add("recording:\"$cleanTitle\"")
        if (track.artist != "Unknown Artist") {
            parts.add("artist:\"${track.artist}\"")
        }
        return parts.joinToString(" AND ")
    }
}
