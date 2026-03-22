package com.yamp.di

import android.content.Context
import androidx.room.Room
import com.yamp.data.local.db.YampDatabase
import com.yamp.data.local.db.dao.LibraryCollectionOrderDao
import com.yamp.data.local.db.dao.ListeningHistoryDao
import com.yamp.data.local.db.dao.MetadataCacheDao
import com.yamp.data.local.db.dao.PlaylistDao
import com.yamp.data.local.db.dao.TrackDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): YampDatabase =
        Room.databaseBuilder(
            context,
            YampDatabase::class.java,
            "yamp_database"
        )
            .addMigrations(YampDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideTrackDao(db: YampDatabase): TrackDao = db.trackDao()

    @Provides
    fun providePlaylistDao(db: YampDatabase): PlaylistDao = db.playlistDao()

    @Provides
    fun provideListeningHistoryDao(db: YampDatabase): ListeningHistoryDao =
        db.listeningHistoryDao()

    @Provides
    fun provideMetadataCacheDao(db: YampDatabase): MetadataCacheDao =
        db.metadataCacheDao()

    @Provides
    fun provideLibraryCollectionOrderDao(db: YampDatabase): LibraryCollectionOrderDao =
        db.libraryCollectionOrderDao()
}
