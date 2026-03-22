package com.yamp.data.local.db

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.yamp.data.local.db.converter.Converters
import com.yamp.data.local.db.dao.LibraryCollectionOrderDao
import com.yamp.data.local.db.dao.ListeningHistoryDao
import com.yamp.data.local.db.dao.MetadataCacheDao
import com.yamp.data.local.db.dao.PlaylistDao
import com.yamp.data.local.db.dao.TrackDao
import com.yamp.data.local.db.entity.LibraryCollectionOrderEntity
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
        MetadataCacheEntity::class,
        LibraryCollectionOrderEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class YampDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun listeningHistoryDao(): ListeningHistoryDao
    abstract fun metadataCacheDao(): MetadataCacheDao
    abstract fun libraryCollectionOrderDao(): LibraryCollectionOrderDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tracks ADD COLUMN sourcePath TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE tracks ADD COLUMN fileHash TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tracks_sourcePath ON tracks(sourcePath)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tracks_fileHash ON tracks(fileHash)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS metadata_cache_new (
                        cacheId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        trackId INTEGER,
                        fileHash TEXT,
                        sourcePath TEXT,
                        musicBrainzRecordingId TEXT,
                        resolvedTitle TEXT,
                        resolvedArtist TEXT,
                        resolvedAlbum TEXT,
                        resolvedGenre TEXT,
                        resolvedYear INTEGER,
                        coverArtUrl TEXT,
                        metadataSource TEXT NOT NULL,
                        fetchedAt INTEGER NOT NULL,
                        confidence REAL NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO metadata_cache_new (
                        trackId,
                        fileHash,
                        sourcePath,
                        musicBrainzRecordingId,
                        resolvedTitle,
                        resolvedArtist,
                        resolvedAlbum,
                        resolvedGenre,
                        resolvedYear,
                        coverArtUrl,
                        metadataSource,
                        fetchedAt,
                        confidence
                    )
                    SELECT
                        trackId,
                        NULL,
                        NULL,
                        musicBrainzRecordingId,
                        resolvedTitle,
                        resolvedArtist,
                        resolvedAlbum,
                        resolvedGenre,
                        resolvedYear,
                        coverArtUrl,
                        'LEGACY_TRACK_ID',
                        fetchedAt,
                        confidence
                    FROM metadata_cache
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE metadata_cache")
                db.execSQL("ALTER TABLE metadata_cache_new RENAME TO metadata_cache")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_metadata_cache_fileHash ON metadata_cache(fileHash)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS library_collection_order (
                        tabType TEXT NOT NULL,
                        itemKey TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        PRIMARY KEY(tabType, itemKey)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_library_collection_order_tabType_sortOrder ON library_collection_order(tabType, sortOrder)"
                )
            }
        }
    }
}
