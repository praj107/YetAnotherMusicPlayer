package com.yamp.data.remote.itunes.dto

data class ITunesSearchResponse(
    val resultCount: Int = 0,
    val results: List<ITunesTrack> = emptyList()
)

data class ITunesTrack(
    val trackId: Long? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val collectionName: String? = null,
    val primaryGenreName: String? = null,
    val releaseDate: String? = null,
    val artworkUrl100: String? = null,
    val trackTimeMillis: Long? = null
)
