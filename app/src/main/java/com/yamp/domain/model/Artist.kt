package com.yamp.domain.model

data class Artist(
    val name: String,
    val trackCount: Int,
    val albumCount: Int,
    val artworkUri: String? = null,
    val tracks: List<Track> = emptyList()
) {
    val key: String
        get() = name
}
