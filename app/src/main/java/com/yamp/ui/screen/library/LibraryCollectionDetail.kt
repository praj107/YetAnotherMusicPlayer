package com.yamp.ui.screen.library

import com.yamp.domain.model.LibraryCollectionType
import com.yamp.domain.model.Track

data class LibraryCollectionDetail(
    val type: LibraryCollectionType,
    val title: String,
    val subtitle: String,
    val artworkUri: String?,
    val tracks: List<Track>
)
