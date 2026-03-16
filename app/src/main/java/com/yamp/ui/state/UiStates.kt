package com.yamp.ui.state

import com.yamp.domain.model.Album
import com.yamp.domain.model.Artist
import com.yamp.domain.model.Genre
import com.yamp.domain.model.Playlist
import com.yamp.domain.model.SortField
import com.yamp.domain.model.SortOrder
import com.yamp.domain.model.Track
import com.yamp.player.RepeatMode

data class HomeUiState(
    val recentTracks: List<Track> = emptyList(),
    val recommendedNext: List<Track> = emptyList(),
    val folderPlaylists: List<Playlist> = emptyList(),
    val isScanning: Boolean = false,
    val scanProgress: String = "",
    val error: String? = null,
    val permissionGranted: Boolean = false
)

data class LibraryUiState(
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val genres: List<Genre> = emptyList(),
    val currentSort: SortOrder = SortOrder(),
    val selectedTab: Int = 0,
    val isLoading: Boolean = false
)

data class SearchUiState(
    val query: String = "",
    val results: List<Track> = emptyList(),
    val isSearching: Boolean = false
)

data class NowPlayingUiState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val queue: List<Track> = emptyList(),
    val queueIndex: Int = 0,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleEnabled: Boolean = false
)

data class PlaylistUiState(
    val playlist: Playlist? = null,
    val isLoading: Boolean = false
)

data class SettingsUiState(
    val autoFetchMetadata: Boolean = false,
    val metadataFetchProgress: String? = null,
    val trackCount: Int = 0,
    val incompleteMetadataCount: Int = 0
)
