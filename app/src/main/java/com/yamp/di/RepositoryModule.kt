package com.yamp.di

import com.yamp.data.repository.ListeningHistoryRepository
import com.yamp.data.repository.ListeningHistoryRepositoryImpl
import com.yamp.data.repository.MetadataRepository
import com.yamp.data.repository.MetadataRepositoryImpl
import com.yamp.data.repository.PlaylistRepository
import com.yamp.data.repository.PlaylistRepositoryImpl
import com.yamp.data.repository.TrackRepository
import com.yamp.data.repository.TrackRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTrackRepository(impl: TrackRepositoryImpl): TrackRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(impl: PlaylistRepositoryImpl): PlaylistRepository

    @Binds
    @Singleton
    abstract fun bindListeningHistoryRepository(
        impl: ListeningHistoryRepositoryImpl
    ): ListeningHistoryRepository

    @Binds
    @Singleton
    abstract fun bindMetadataRepository(impl: MetadataRepositoryImpl): MetadataRepository
}
