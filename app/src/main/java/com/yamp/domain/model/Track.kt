package com.yamp.domain.model

data class Track(
    val id: Long,
    val contentUri: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtUri: String?,
    val genre: String?,
    val duration: Long,
    val trackNumber: Int?,
    val year: Int?,
    val mimeType: String,
    val folderPath: String,
    val metadataComplete: Boolean,
    val sourcePath: String = "",
    val fileHash: String? = null
) {
    val durationFormatted: String
        get() {
            val minutes = duration / 1000 / 60
            val seconds = (duration / 1000) % 60
            return "%d:%02d".format(minutes, seconds)
        }
}
