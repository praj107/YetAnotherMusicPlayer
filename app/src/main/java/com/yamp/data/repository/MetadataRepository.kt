package com.yamp.data.repository

import com.yamp.domain.model.Track

interface MetadataRepository {
    suspend fun fetchAndCacheMetadata(track: Track): Track?
    suspend fun getIncompleteMetadataTracks(): List<Track>
    suspend fun batchFetchMetadata(tracks: List<Track>, onProgress: (Int, Int) -> Unit)
    suspend fun hydrateCachedMetadata(trackIds: List<Long>)
}
