package com.yamp.data.mapper

import com.yamp.data.local.db.entity.TrackEntity
import com.yamp.domain.model.Track

fun TrackEntity.toDomain(): Track = Track(
    id = id,
    contentUri = contentUri,
    title = title,
    artist = artist ?: "Unknown Artist",
    album = album ?: "Unknown Album",
    albumArtUri = albumArtUri,
    genre = genre,
    duration = duration,
    trackNumber = trackNumber,
    year = year,
    mimeType = mimeType,
    folderPath = folderPath,
    metadataComplete = metadataComplete
)

fun List<TrackEntity>.toDomain(): List<Track> = map { it.toDomain() }
