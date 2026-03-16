package com.yamp.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.yamp.data.local.db.converter.Converters
import com.yamp.data.local.db.dao.ListeningHistoryDao
import com.yamp.data.local.db.dao.MetadataCacheDao
import com.yamp.data.local.db.dao.PlaylistDao
import com.yamp.data.local.db.dao.TrackDao
import com.yamp.data.local.db.entity.ListeningHistoryEntity
import com.yamp.data.local.db.entity.MetadataCacheEntity
import com.yamp.data.local.db.entity.PlaylistEntity
import com.yamp.data.local.db.entity.PlaylistTrackCrossRef
import com.yamp.data.local.db.entity.TrackEntity

@Database(
    entities = [
        TrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        ListeningHistoryEntity::class,
        MetadataCacheEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class YampDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun listeningHistoryDao(): ListeningHistoryDao
    abstract fun metadataCacheDao(): MetadataCacheDao
}
