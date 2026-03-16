package com.yamp.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yamp.data.local.db.entity.MetadataCacheEntity

@Dao
interface MetadataCacheDao {

    @Query("SELECT * FROM metadata_cache WHERE trackId = :trackId")
    suspend fun getCachedMetadata(trackId: Long): MetadataCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(cache: MetadataCacheEntity)

    @Query("DELETE FROM metadata_cache WHERE fetchedAt < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long)

    @Query("SELECT COUNT(*) FROM metadata_cache")
    suspend fun getCacheSize(): Int
}
