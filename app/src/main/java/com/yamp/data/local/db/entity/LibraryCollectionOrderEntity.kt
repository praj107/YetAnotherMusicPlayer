package com.yamp.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "library_collection_order",
    primaryKeys = ["tabType", "itemKey"],
    indices = [Index(value = ["tabType", "sortOrder"])]
)
data class LibraryCollectionOrderEntity(
    val tabType: String,
    val itemKey: String,
    val sortOrder: Int
)
