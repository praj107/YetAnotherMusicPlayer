package com.yamp.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "metadata_cache")
data class MetadataCacheEntity(
    @PrimaryKey val trackId: Long,
    val musicBrainzRecordingId: String?,
    val resolvedTitle: String?,
    val resolvedArtist: String?,
    val resolvedAlbum: String?,
    val resolvedGenre: String?,
    val resolvedYear: Int?,
    val coverArtUrl: String?,
    val fetchedAt: Long,
    val confidence: Float
)
