package com.yamp.di

import com.yamp.data.remote.itunes.ITunesSearchApi
import com.yamp.data.remote.musicbrainz.MusicBrainzApi
import com.yamp.data.remote.musicbrainz.MusicBrainzRateLimiter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val MUSICBRAINZ_BASE_URL = "https://musicbrainz.org/ws/2/"
    private const val ITUNES_BASE_URL = "https://itunes.apple.com/"

    @Provides
    @Singleton
    fun provideUserAgentInterceptor(): Interceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("User-Agent", "YAMP/1.0.0 (yamp-music-player)")
            .header("Accept", "application/json")
            .build()
        chain.proceed(request)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(userAgentInterceptor: Interceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(userAgentInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideMusicBrainzApi(okHttpClient: OkHttpClient): MusicBrainzApi =
        Retrofit.Builder()
            .baseUrl(MUSICBRAINZ_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MusicBrainzApi::class.java)

    @Provides
    @Singleton
    fun provideITunesSearchApi(okHttpClient: OkHttpClient): ITunesSearchApi =
        Retrofit.Builder()
            .baseUrl(ITUNES_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ITunesSearchApi::class.java)

    @Provides
    @Singleton
    fun provideRateLimiter(): MusicBrainzRateLimiter = MusicBrainzRateLimiter()
}
