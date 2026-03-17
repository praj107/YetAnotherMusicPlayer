package com.yamp.data.mapper

import com.yamp.data.local.db.entity.MetadataCacheEntity
import com.yamp.data.remote.itunes.dto.ITunesTrack
import com.yamp.data.remote.musicbrainz.dto.MBRecording

fun MBRecording.toMetadataCache(trackId: Long): MetadataCacheEntity = MetadataCacheEntity(
    trackId = trackId,
    musicBrainzRecordingId = id,
    resolvedTitle = title,
    resolvedArtist = artistCredit.firstOrNull()?.artist?.name,
    resolvedAlbum = releases?.firstOrNull()?.title,
    resolvedGenre = null,
    resolvedYear = releases?.firstOrNull()?.date?.take(4)?.toIntOrNull(),
    coverArtUrl = releases?.firstOrNull()?.let { "https://coverartarchive.org/release/${it.id}/front-250" },
    fetchedAt = System.currentTimeMillis(),
    confidence = score / 100f
)

fun ITunesTrack.toMetadataCache(trackId: Long): MetadataCacheEntity = MetadataCacheEntity(
    trackId = trackId,
    musicBrainzRecordingId = null,
    resolvedTitle = trackName,
    resolvedArtist = artistName,
    resolvedAlbum = collectionName,
    resolvedGenre = primaryGenreName,
    resolvedYear = releaseDate?.take(4)?.toIntOrNull(),
    coverArtUrl = artworkUrl100?.replace("100x100", "600x600"),
    fetchedAt = System.currentTimeMillis(),
    confidence = 0.85f
)
