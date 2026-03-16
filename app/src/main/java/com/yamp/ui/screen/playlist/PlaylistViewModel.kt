package com.yamp.ui.screen.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yamp.data.repository.PlaylistRepository
import com.yamp.ui.state.PlaylistUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val playlistId: Long = savedStateHandle.get<Long>("playlistId") ?: -1L

    private val _uiState = MutableStateFlow(PlaylistUiState(isLoading = true))
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    init {
        loadPlaylist()
    }

    private fun loadPlaylist() {
        viewModelScope.launch {
            val playlist = playlistRepository.getPlaylistById(playlistId)
            _uiState.update { it.copy(playlist = playlist, isLoading = false) }
        }
    }
}
