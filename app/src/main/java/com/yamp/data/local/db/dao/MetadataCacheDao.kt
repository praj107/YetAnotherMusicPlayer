package com.yamp.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yamp.data.local.db.entity.MetadataCacheEntity

@Dao
interface MetadataCacheDao {

    @Query("SELECT * FROM metadata_cache WHERE trackId = :trackId ORDER BY fetchedAt DESC LIMIT 1")
    suspend fun getCachedMetadataByTrackId(trackId: Long): MetadataCacheEntity?

    @Query("SELECT * FROM metadata_cache WHERE fileHash = :fileHash LIMIT 1")
    suspend fun getCachedMetadataByFileHash(fileHash: String): MetadataCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(cache: MetadataCacheEntity)

    @Query("DELETE FROM metadata_cache WHERE trackId = :trackId AND fileHash IS NULL")
    suspend fun deleteLegacyTrackCache(trackId: Long)

    @Query("DELETE FROM metadata_cache WHERE fetchedAt < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long)

    @Query("SELECT COUNT(*) FROM metadata_cache")
    suspend fun getCacheSize(): Int
}
