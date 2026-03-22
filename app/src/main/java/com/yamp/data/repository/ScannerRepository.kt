package com.yamp.data.repository

import com.yamp.data.local.db.dao.TrackDao
import com.yamp.data.scanner.MediaStoreScanner
import com.yamp.crash.YampDiagnostics
import javax.inject.Inject

class ScannerRepository @Inject constructor(
    private val scanner: MediaStoreScanner,
    private val trackDao: TrackDao,
    private val playlistRepository: PlaylistRepository,
    private val metadataRepository: MetadataRepository
) {
    suspend fun scanAndPersist(): Int {
        YampDiagnostics.i("ScannerRepository", "Starting media scan")
        val result = scanner.scan()
        if (result.tracks.isEmpty()) return 0

        val existingTracks = trackDao.getAllTracksSnapshot()
        val existingById = existingTracks.associateBy { it.id }
        val existingBySourcePath = existingTracks.associateBy { it.sourcePath }
        val mergedTracks = result.tracks.map { scanned ->
            mergeTrack(scanned, existingById[scanned.id] ?: existingBySourcePath[scanned.sourcePath])
        }

        trackDao.upsertTracks(mergedTracks)

        val activeIds = mergedTracks.map { it.id }
        trackDao.deleteStale(activeIds)
        metadataRepository.hydrateCachedMetadata(activeIds)

        val hydratedTracks = trackDao.getTracksByIds(activeIds)
        val tracksByFolder = hydratedTracks.groupBy { it.folderPath }
        for ((folderPath, folderTracks) in tracksByFolder) {
            playlistRepository.createOrUpdateFolderPlaylist(
                folderPath = folderPath,
                trackIds = folderTracks.map { it.id }
            )
        }

        YampDiagnostics.i("ScannerRepository", "Scan persisted ${mergedTracks.size} tracks")
        return mergedTracks.size
    }

    private fun mergeTrack(
        scanned: com.yamp.data.local.db.entity.TrackEntity,
        existing: com.yamp.data.local.db.entity.TrackEntity?
    ): com.yamp.data.local.db.entity.TrackEntity {
        if (existing == null) return scanned

        val sameContent = existing.fileSize == scanned.fileSize &&
            existing.dateModified == scanned.dateModified
        val preserveRichMetadata = sameContent && existing.metadataComplete

        return scanned.copy(
            title = if (preserveRichMetadata) existing.title else scanned.title,
            artist = existing.artist ?: scanned.artist,
            album = existing.album ?: scanned.album,
            albumArtUri = existing.albumArtUri ?: scanned.albumArtUri,
            genre = existing.genre ?: scanned.genre,
            year = existing.year ?: scanned.year,
            musicBrainzId = if (sameContent) existing.musicBrainzId else null,
            metadataComplete = existing.metadataComplete || scanned.metadataComplete,
            sourcePath = scanned.sourcePath.ifBlank { existing.sourcePath },
            fileHash = if (sameContent) existing.fileHash else null
        )
    }
}
