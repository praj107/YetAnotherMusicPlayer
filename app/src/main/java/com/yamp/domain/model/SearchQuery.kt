package com.yamp.domain.model

data class SearchQuery(
    val text: String,
    val fields: Set<SortField> = SortField.entries.toSet()
)
