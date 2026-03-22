package com.yamp.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tracks",
    indices = [
        Index("sourcePath"),
        Index("fileHash")
    ]
)
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
    val metadataComplete: Boolean,
    val sourcePath: String = "",
    val fileHash: String? = null
)
