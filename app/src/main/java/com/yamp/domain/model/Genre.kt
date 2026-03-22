package com.yamp.domain.model

data class Genre(
    val name: String,
    val trackCount: Int,
    val artworkUri: String? = null,
    val tracks: List<Track> = emptyList()
) {
    val key: String
        get() = name
}
