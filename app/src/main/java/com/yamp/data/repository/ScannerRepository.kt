package com.yamp.data.repository

import com.yamp.data.local.db.dao.TrackDao
import com.yamp.data.scanner.MediaStoreScanner
import javax.inject.Inject

class ScannerRepository @Inject constructor(
    private val scanner: MediaStoreScanner,
    private val trackDao: TrackDao,
    private val playlistRepository: PlaylistRepository
) {
    suspend fun scanAndPersist(): Int {
        val result = scanner.scan()
        if (result.tracks.isEmpty()) return 0

        trackDao.upsertTracks(result.tracks)

        val activeIds = result.tracks.map { it.id }
        trackDao.deleteStale(activeIds)

        val tracksByFolder = result.tracks.groupBy { it.folderPath }
        for ((folderPath, folderTracks) in tracksByFolder) {
            playlistRepository.createOrUpdateFolderPlaylist(
                folderPath = folderPath,
                trackIds = folderTracks.map { it.id }
            )
        }

        return result.tracks.size
    }
}
