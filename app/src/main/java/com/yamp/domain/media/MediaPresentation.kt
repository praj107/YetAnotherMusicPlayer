package com.yamp.domain.media

import com.yamp.domain.model.Track

fun pickRepresentativeArtwork(tracks: List<Track>): String? =
    tracks.asSequence()
        .mapNotNull { it.albumArtUri?.takeIf(String::isNotBlank) }
        .groupingBy { it }
        .eachCount()
        .maxWithOrNull(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        ?.key

fun trackFormatBadge(sourcePath: String, mimeType: String): String? {
    val extension = sourcePath.substringAfterLast('.', "").uppercase()
    if (extension.isNotBlank()) return extension.take(4)

    val subtype = mimeType.substringAfter('/', "").uppercase()
    return subtype.takeIf { it.isNotBlank() }?.take(4)
}
