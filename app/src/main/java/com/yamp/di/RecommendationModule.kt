package com.yamp.di

import com.yamp.recommendation.HistoryBasedRecommendationEngine
import com.yamp.recommendation.RecommendationEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RecommendationModule {

    @Binds
    @Singleton
    abstract fun bindRecommendationEngine(
        impl: HistoryBasedRecommendationEngine
    ): RecommendationEngine
}
