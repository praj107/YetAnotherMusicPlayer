package com.yamp.data.repository

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.yamp.crash.YampDiagnostics
import com.yamp.data.local.db.dao.MetadataCacheDao
import com.yamp.data.local.db.dao.TrackDao
import com.yamp.data.local.db.entity.MetadataCacheEntity
import com.yamp.data.mapper.toDomain
import com.yamp.data.mapper.toMetadataCache
import com.yamp.data.remote.itunes.ITunesSearchApi
import com.yamp.data.remote.musicbrainz.MusicBrainzApi
import com.yamp.data.remote.musicbrainz.MusicBrainzRateLimiter
import com.yamp.domain.model.Track
import java.security.MessageDigest
import javax.inject.Inject

class MetadataRepositoryImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    private val musicBrainzApi: MusicBrainzApi,
    private val iTunesSearchApi: ITunesSearchApi,
    private val metadataCacheDao: MetadataCacheDao,
    private val trackDao: TrackDao,
    private val rateLimiter: MusicBrainzRateLimiter
) : MetadataRepository {

    companion object {
        private const val TAG = "MetadataRepository"
        private const val MIN_CONFIDENCE = 0.60f
        private const val SOURCE_ITUNES = "ITUNES"
        private const val SOURCE_MUSIC_BRAINZ = "MUSIC_BRAINZ"
        private const val SOURCE_LEGACY = "LEGACY_TRACK_ID"
    }

    override suspend fun fetchAndCacheMetadata(track: Track): Track? {
        val fileHash = ensureFileHash(track)
        val cached = findBestCache(track.id, fileHash)
        if (cached != null && cached.confidence >= MIN_CONFIDENCE) {
            YampDiagnostics.i(TAG, "Cache hit for ${track.title}")
            if (fileHash != null && cached.fileHash == null) {
                promoteLegacyCache(track.id, fileHash, track.sourcePath, cached)
            }
            applyCachedMetadata(track.id, fileHash, cached)
            return track.copy(
                title = cached.resolvedTitle ?: track.title,
                artist = cached.resolvedArtist ?: track.artist,
                album = cached.resolvedAlbum ?: track.album,
                albumArtUri = cached.coverArtUrl ?: track.albumArtUri,
                genre = cached.resolvedGenre ?: track.genre,
                year = cached.resolvedYear ?: track.year,
                metadataComplete = true,
                fileHash = fileHash ?: track.fileHash
            )
        }

        val resolvedHash = fileHash ?: return null
        val searchTerm = buildSearchTerm(track)

        val iTunesResult = tryITunes(track, resolvedHash, searchTerm)
        if (iTunesResult != null) return iTunesResult

        val mbResult = tryMusicBrainz(track, resolvedHash, searchTerm)
        if (mbResult != null) return mbResult

        val simpleTerm = cleanTitleForSearch(track.title)
        if (simpleTerm != searchTerm) {
            val iTunesRetry = tryITunes(track, resolvedHash, simpleTerm)
            if (iTunesRetry != null) return iTunesRetry
        }

        return null
    }

    override suspend fun getIncompleteMetadataTracks(): List<Track> =
        trackDao.getIncompleteMetadataTracks().toDomain()

    override suspend fun batchFetchMetadata(tracks: List<Track>, onProgress: (Int, Int) -> Unit) {
        tracks.forEachIndexed { index, track ->
            fetchAndCacheMetadata(track)
            onProgress(index + 1, tracks.size)
        }
    }

    override suspend fun hydrateCachedMetadata(trackIds: List<Long>) {
        trackDao.getTracksByIds(trackIds).forEach { entity ->
            val shouldHydrate = !entity.metadataComplete || entity.albumArtUri.isNullOrBlank() || entity.fileHash != null
            if (!shouldHydrate) return@forEach

            val fileHash = entity.fileHash ?: computeFileHash(entity.contentUri)?.also {
                trackDao.updateFileHash(entity.id, it)
            }
            val cached = findBestCache(entity.id, fileHash) ?: return@forEach
            if (cached.confidence < MIN_CONFIDENCE) return@forEach

            if (fileHash != null && cached.fileHash == null) {
                promoteLegacyCache(entity.id, fileHash, entity.sourcePath, cached)
            }
            applyCachedMetadata(entity.id, fileHash, cached)
        }
    }

    private suspend fun tryITunes(track: Track, fileHash: String, searchTerm: String): Track? {
        return try {
            val response = iTunesSearchApi.search(term = searchTerm, limit = 5)
            val bestMatch = response.results.firstOrNull { result ->
                result.trackName != null && result.artistName != null
            } ?: return null

            val cacheEntity = bestMatch.toMetadataCache(
                trackId = track.id,
                fileHash = fileHash,
                sourcePath = track.sourcePath,
                metadataSource = SOURCE_ITUNES
            )
            metadataCacheDao.insertOrUpdate(cacheEntity)
            applyCachedMetadata(track.id, fileHash, cacheEntity)
            YampDiagnostics.i(TAG, "Fetched iTunes metadata for ${track.title}")

            track.copy(
                title = cacheEntity.resolvedTitle ?: track.title,
                artist = cacheEntity.resolvedArtist ?: track.artist,
                album = cacheEntity.resolvedAlbum ?: track.album,
                albumArtUri = cacheEntity.coverArtUrl ?: track.albumArtUri,
                genre = cacheEntity.resolvedGenre ?: track.genre,
                year = cacheEntity.resolvedYear ?: track.year,
                metadataComplete = true,
                fileHash = fileHash
            )
        } catch (e: Exception) {
            Log.w(TAG, "iTunes search failed for '${track.title}': ${e.message}")
            null
        }
    }

    private suspend fun tryMusicBrainz(track: Track, fileHash: String, searchTerm: String): Track? {
        return try {
            val query = "recording:\"$searchTerm\""
            val response = rateLimiter.throttle {
                musicBrainzApi.searchRecordings(query)
            }

            val bestMatch = response.recordings
                ?.filter { it.score >= (MIN_CONFIDENCE * 100).toInt() }
                ?.maxByOrNull { it.score }
                ?: return null

            val cacheEntity = bestMatch.toMetadataCache(
                trackId = track.id,
                fileHash = fileHash,
                sourcePath = track.sourcePath,
                metadataSource = SOURCE_MUSIC_BRAINZ
            )
            metadataCacheDao.insertOrUpdate(cacheEntity)
            applyCachedMetadata(track.id, fileHash, cacheEntity)
            YampDiagnostics.i(TAG, "Fetched MusicBrainz metadata for ${track.title}")

            track.copy(
                title = cacheEntity.resolvedTitle ?: track.title,
                artist = cacheEntity.resolvedArtist ?: track.artist,
                album = cacheEntity.resolvedAlbum ?: track.album,
                albumArtUri = cacheEntity.coverArtUrl ?: track.albumArtUri,
                genre = cacheEntity.resolvedGenre ?: track.genre,
                year = cacheEntity.resolvedYear ?: track.year,
                metadataComplete = true,
                fileHash = fileHash
            )
        } catch (e: Exception) {
            Log.w(TAG, "MusicBrainz search failed for '${track.title}': ${e.message}")
            null
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
            .replace(Regex("\\.(mp3|flac|ogg|wav|m4a|aac|opus|wma|alac)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^\\d{1,3}[.\\-\\s]+\\s*"), "")
            .replace('_', ' ')
            .replace(Regex("\\s*-{2,}\\s*"), " ")
            .replace(
                Regex(
                    "\\s*[\\[(](?:official|lyric|audio|video|feat|ft|remix|edit|version|hq|hd|4k|1080p|720p|music\\s*video).*?[)\\]]",
                    RegexOption.IGNORE_CASE
                ),
                ""
            )
            .replace(Regex("\\s+"), " ")
            .trim()

        if (cleaned.contains(" - ")) {
            val parts = cleaned.split(" - ", limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                cleaned = "${parts[0].trim()} ${parts[1].trim()}"
            }
        }

        return cleaned
    }

    private suspend fun findBestCache(trackId: Long, fileHash: String?): MetadataCacheEntity? {
        if (fileHash != null) {
            metadataCacheDao.getCachedMetadataByFileHash(fileHash)?.let { return it }
        }
        return metadataCacheDao.getCachedMetadataByTrackId(trackId)
    }

    private suspend fun ensureFileHash(track: Track): String? {
        track.fileHash?.let { return it }
        val hash = computeFileHash(track.contentUri) ?: return null
        trackDao.updateFileHash(track.id, hash)
        return hash
    }

    private fun computeFileHash(contentUri: String): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            contentResolver.openInputStream(Uri.parse(contentUri))?.use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    digest.update(buffer, 0, read)
                }
            } ?: return null

            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to hash $contentUri: ${e.message}")
            YampDiagnostics.w(TAG, "Unable to hash $contentUri", e)
            null
        }
    }

    private suspend fun promoteLegacyCache(
        trackId: Long,
        fileHash: String,
        sourcePath: String,
        cached: MetadataCacheEntity
    ) {
        metadataCacheDao.insertOrUpdate(
            cached.copy(
                cacheId = 0,
                trackId = trackId,
                fileHash = fileHash,
                sourcePath = sourcePath,
                metadataSource = cached.metadataSource
            )
        )
        metadataCacheDao.deleteLegacyTrackCache(trackId)
    }

    private suspend fun applyCachedMetadata(
        trackId: Long,
        fileHash: String?,
        cached: MetadataCacheEntity
    ) {
        trackDao.updateMetadata(
            trackId = trackId,
            fileHash = fileHash,
            mbid = cached.musicBrainzRecordingId,
            title = cached.resolvedTitle,
            artist = cached.resolvedArtist,
            album = cached.resolvedAlbum,
            albumArtUri = cached.coverArtUrl,
            genre = cached.resolvedGenre,
            year = cached.resolvedYear,
            metadataComplete = true
        )
    }
}
