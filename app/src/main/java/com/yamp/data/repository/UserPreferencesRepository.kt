package com.yamp.data.repository

import android.content.Context
import com.yamp.data.local.datastore.UserPreferences
import com.yamp.data.local.datastore.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val userPreferences = UserPreferences(context.dataStore)

    val autoFetchMetadata: Flow<Boolean> = userPreferences.autoFetchMetadata

    suspend fun setAutoFetchMetadata(enabled: Boolean) {
        userPreferences.setAutoFetchMetadata(enabled)
    }
}
