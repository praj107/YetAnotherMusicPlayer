package com.yamp.ui.screen.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yamp.data.local.db.dao.TrackDao
import com.yamp.data.repository.TrackRepository
import com.yamp.domain.model.Album
import com.yamp.domain.model.Artist
import com.yamp.domain.model.Genre
import com.yamp.domain.model.SortDirection
import com.yamp.domain.model.SortField
import com.yamp.domain.model.SortOrder
import com.yamp.ui.state.LibraryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val trackRepository: TrackRepository,
    private val trackDao: TrackDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState(isLoading = true))
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadTracks()
        loadAlbums()
        loadArtists()
        loadGenres()
    }

    private fun loadTracks() {
        viewModelScope.launch {
            trackRepository.getTracksSorted(_uiState.value.currentSort).collect { tracks ->
                _uiState.update { it.copy(tracks = tracks, isLoading = false) }
            }
        }
    }

    private fun loadAlbums() {
        viewModelScope.launch {
            trackRepository.getAllTracks().collect { tracks ->
                val albums = tracks.groupBy { it.album }.map { (albumName, albumTracks) ->
                    Album(
                        name = albumName,
                        artist = albumTracks.firstOrNull()?.artist ?: "Unknown",
                        albumArtUri = albumTracks.firstOrNull()?.albumArtUri,
                        year = albumTracks.firstOrNull()?.year,
                        tracks = albumTracks
                    )
                }.sortedBy { it.name }
                _uiState.update { it.copy(albums = albums) }
            }
        }
    }

    private fun loadArtists() {
        viewModelScope.launch {
            trackDao.getAllArtistNames().collect { names ->
                val artists = names.map { name ->
                    Artist(
                        name = name,
                        trackCount = trackDao.getTrackCountForArtist(name),
                        albumCount = trackDao.getAlbumCountForArtist(name)
                    )
                }
                _uiState.update { it.copy(artists = artists) }
            }
        }
    }

    private fun loadGenres() {
        viewModelScope.launch {
            trackDao.getAllGenreNames().collect { names ->
                val genres = names.map { name ->
                    Genre(
                        name = name,
                        trackCount = trackDao.getTrackCountForGenre(name)
                    )
                }
                _uiState.update { it.copy(genres = genres) }
            }
        }
    }

    fun onSortFieldSelected(field: SortField) {
        val currentSort = _uiState.value.currentSort
        val newDirection = if (currentSort.field == field) {
            if (currentSort.direction == SortDirection.ASCENDING) SortDirection.DESCENDING
            else SortDirection.ASCENDING
        } else SortDirection.ASCENDING

        _uiState.update { it.copy(currentSort = SortOrder(field, newDirection)) }
        loadTracks()
    }

    fun onTabSelected(tab: Int) {
        _uiState.update { it.copy(selectedTab = tab) }
    }
}
