package com.yamp.domain.model

enum class SortField {
    TITLE, ARTIST, ALBUM, GENRE, DURATION, DATE_ADDED
}

enum class SortDirection {
    ASCENDING, DESCENDING
}

data class SortOrder(
    val field: SortField = SortField.TITLE,
    val direction: SortDirection = SortDirection.ASCENDING
)
