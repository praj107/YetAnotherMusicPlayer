package com.yamp.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "metadata_cache",
    indices = [Index(value = ["fileHash"], unique = true)]
)
data class MetadataCacheEntity(
    @PrimaryKey(autoGenerate = true) val cacheId: Long = 0,
    val trackId: Long?,
    val fileHash: String?,
    val sourcePath: String?,
    val musicBrainzRecordingId: String?,
    val resolvedTitle: String?,
    val resolvedArtist: String?,
    val resolvedAlbum: String?,
    val resolvedGenre: String?,
    val resolvedYear: Int?,
    val coverArtUrl: String?,
    val metadataSource: String,
    val fetchedAt: Long,
    val confidence: Float
)
