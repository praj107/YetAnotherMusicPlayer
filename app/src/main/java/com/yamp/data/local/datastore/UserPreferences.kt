package com.yamp.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "yamp_preferences")

object PreferenceKeys {
    val SORT_FIELD = stringPreferencesKey("sort_field")
    val SORT_DIRECTION = stringPreferencesKey("sort_direction")
    val AUTO_FETCH_METADATA = booleanPreferencesKey("auto_fetch_metadata")
    val LAST_SCAN_TIMESTAMP = intPreferencesKey("last_scan_timestamp")
}

class UserPreferences(private val dataStore: DataStore<Preferences>) {

    val autoFetchMetadata: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PreferenceKeys.AUTO_FETCH_METADATA] ?: false
    }

    val sortField: Flow<String> = dataStore.data.map { prefs ->
        prefs[PreferenceKeys.SORT_FIELD] ?: "TITLE"
    }

    val sortDirection: Flow<String> = dataStore.data.map { prefs ->
        prefs[PreferenceKeys.SORT_DIRECTION] ?: "ASCENDING"
    }

    suspend fun setAutoFetchMetadata(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.AUTO_FETCH_METADATA] = enabled
        }
    }

    suspend fun setSortField(field: String) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.SORT_FIELD] = field
        }
    }

    suspend fun setSortDirection(direction: String) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.SORT_DIRECTION] = direction
        }
    }
}
