package com.yamp.domain.model

data class Album(
    val name: String,
    val artist: String,
    val albumArtUri: String?,
    val year: Int?,
    val tracks: List<Track>
)
