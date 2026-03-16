package com.yamp.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: Long,
    val contentUri: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val albumArtUri: String?,
    val genre: String?,
    val duration: Long,
    val trackNumber: Int?,
    val year: Int?,
    val fileSize: Long,
    val mimeType: String,
    val folderPath: String,
    val dateAdded: Long,
    val dateModified: Long,
    val musicBrainzId: String?,
    val metadataComplete: Boolean
)
