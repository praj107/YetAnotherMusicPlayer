package com.yamp.ui.screen.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yamp.player.PlaybackManager
import com.yamp.player.PlaybackState
import com.yamp.ui.state.NowPlayingUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val playbackManager: PlaybackManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(NowPlayingUiState())
    val uiState: StateFlow<NowPlayingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            playbackManager.playbackState.collect { state ->
                _uiState.update { ui ->
                    when (state) {
                        is PlaybackState.Playing -> ui.copy(
                            currentTrack = state.track,
                            isPlaying = true,
                            positionMs = state.positionMs,
                            durationMs = state.durationMs,
                            queue = state.queue,
                            queueIndex = state.queueIndex
                        )
                        is PlaybackState.Paused -> ui.copy(
                            currentTrack = state.track,
                            isPlaying = false,
                            positionMs = state.positionMs,
                            durationMs = state.durationMs,
                            queue = state.queue,
                            queueIndex = state.queueIndex
                        )
                        is PlaybackState.Buffering -> ui.copy(
                            currentTrack = state.track
                        )
                        is PlaybackState.Idle -> NowPlayingUiState()
                        is PlaybackState.Error -> ui.copy(isPlaying = false)
                    }
                }
            }
        }
    }

    fun onPlayPause() {
        if (_uiState.value.isPlaying) playbackManager.pause() else playbackManager.play()
    }

    fun onNext() = playbackManager.skipNext()
    fun onPrevious() = playbackManager.skipPrevious()
    fun onSeek(positionMs: Long) = playbackManager.seekTo(positionMs)

    fun onToggleRepeat() {
        val mode = playbackManager.toggleRepeat()
        _uiState.update { it.copy(repeatMode = mode) }
    }

    fun onToggleShuffle() {
        val enabled = playbackManager.toggleShuffle()
        _uiState.update { it.copy(shuffleEnabled = enabled) }
    }
}
