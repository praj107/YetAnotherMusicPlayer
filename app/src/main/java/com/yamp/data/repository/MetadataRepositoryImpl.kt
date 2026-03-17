package com.yamp.data.repository

import android.util.Log
import com.yamp.data.local.db.dao.MetadataCacheDao
import com.yamp.data.local.db.dao.TrackDao
import com.yamp.data.mapper.toDomain
import com.yamp.data.mapper.toMetadataCache
import com.yamp.data.remote.itunes.ITunesSearchApi
import com.yamp.data.remote.musicbrainz.MusicBrainzApi
import com.yamp.data.remote.musicbrainz.MusicBrainzRateLimiter
import com.yamp.domain.model.Track
import javax.inject.Inject

class MetadataRepositoryImpl @Inject constructor(
    private val musicBrainzApi: MusicBrainzApi,
    private val iTunesSearchApi: ITunesSearchApi,
    private val metadataCacheDao: MetadataCacheDao,
    private val trackDao: TrackDao,
    private val rateLimiter: MusicBrainzRateLimiter
) : MetadataRepository {

    companion object {
        private const val TAG = "MetadataRepository"
        private const val MIN_CONFIDENCE = 0.60f
    }

    override suspend fun fetchAndCacheMetadata(track: Track): Track? {
        // Check cache first
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

        // Parse the filename/title into a clean search term
        val searchTerm = buildSearchTerm(track)

        // Try iTunes first (fast, reliable, returns genre)
        val iTunesResult = tryITunes(track, searchTerm)
        if (iTunesResult != null) return iTunesResult

        // Fallback: try MusicBrainz
        val mbResult = tryMusicBrainz(track, searchTerm)
        if (mbResult != null) return mbResult

        // Last resort: try with just the cleaned title words
        val simpleTerm = cleanTitleForSearch(track.title)
        if (simpleTerm != searchTerm) {
            val iTunesRetry = tryITunes(track, simpleTerm)
            if (iTunesRetry != null) return iTunesRetry
        }

        return null
    }

    private suspend fun tryITunes(track: Track, searchTerm: String): Track? {
        return try {
            val response = iTunesSearchApi.search(term = searchTerm, limit = 5)
            val bestMatch = response.results.firstOrNull { result ->
                result.trackName != null && result.artistName != null
            } ?: return null

            val cacheEntity = bestMatch.toMetadataCache(track.id)
            metadataCacheDao.insertOrUpdate(cacheEntity)

            trackDao.updateMetadata(
                trackId = track.id,
                mbid = null,
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
        } catch (e: Exception) {
            Log.w(TAG, "iTunes search failed for '${track.title}': ${e.message}")
            null
        }
    }

    private suspend fun tryMusicBrainz(track: Track, searchTerm: String): Track? {
        return try {
            val query = "recording:\"$searchTerm\""
            val response = rateLimiter.throttle {
                musicBrainzApi.searchRecordings(query)
            }

            val bestMatch = response.recordings
                ?.filter { it.score >= (MIN_CONFIDENCE * 100).toInt() }
                ?.maxByOrNull { it.score }
                ?: return null

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
        } catch (e: Exception) {
            Log.w(TAG, "MusicBrainz search failed for '${track.title}': ${e.message}")
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

    /**
     * Builds a search term from track metadata.
     * Handles various filename patterns:
     *   "Artist - Title.mp3"
     *   "01 - Title.mp3"
     *   "01. Artist - Title.flac"
     *   "Title (feat. Other).mp3"
     *   "some_song_name.mp3"
     */
    private fun buildSearchTerm(track: Track): String {
        val hasRealArtist = track.artist != "Unknown Artist" &&
                track.artist.isNotBlank() &&
                !track.artist.equals("<unknown>", ignoreCase = true)

        val cleanTitle = cleanTitleForSearch(track.title)

        return if (hasRealArtist) {
            "${track.artist} $cleanTitle"
        } else {
            cleanTitle
        }
    }

    private fun cleanTitleForSearch(raw: String): String {
        var cleaned = raw
            // Remove file extension
            .replace(Regex("\\.(mp3|flac|ogg|wav|m4a|aac|opus|wma|alac)$", RegexOption.IGNORE_CASE), "")
            // Remove leading track numbers: "01 - ", "01. ", "1 ", "01-"
            .replace(Regex("^\\d{1,3}[.\\-\\s]+\\s*"), "")
            // Replace underscores and multiple hyphens with spaces
            .replace('_', ' ')
            .replace(Regex("\\s*-{2,}\\s*"), " ")
            // Remove common bracket suffixes: (Official Video), [Lyrics], (feat. X)
            .replace(Regex("\\s*[\\[(](?:official|lyric|audio|video|feat|ft|remix|edit|version|hq|hd|4k|1080p|720p|music\\s*video).*?[)\\]]", RegexOption.IGNORE_CASE), "")
            // Clean up multiple spaces
            .replace(Regex("\\s+"), " ")
            .trim()

        // If the filename contains " - " (Artist - Title pattern), use both parts
        if (cleaned.contains(" - ")) {
            val parts = cleaned.split(" - ", limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                cleaned = "${parts[0].trim()} ${parts[1].trim()}"
            }
        }

        return cleaned
    }
}
