package com.yamp.ui.screen.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yamp.data.local.datastore.UserPreferences
import com.yamp.data.local.datastore.dataStore
import com.yamp.data.repository.MetadataRepository
import com.yamp.data.repository.TrackRepository
import com.yamp.domain.usecase.metadata.FetchMetadataUseCase
import com.yamp.domain.usecase.scan.ScanDeviceUseCase
import com.yamp.ui.state.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val scanDeviceUseCase: ScanDeviceUseCase,
    private val fetchMetadataUseCase: FetchMetadataUseCase,
    private val trackRepository: TrackRepository,
    private val metadataRepository: MetadataRepository
) : ViewModel() {

    private val userPreferences = UserPreferences(context.dataStore)
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferences.autoFetchMetadata.collect { enabled ->
                _uiState.update { it.copy(autoFetchMetadata = enabled) }
            }
        }
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            val count = trackRepository.getTrackCount()
            val incomplete = metadataRepository.getIncompleteMetadataTracks().size
            _uiState.update { it.copy(trackCount = count, incompleteMetadataCount = incomplete) }
        }
    }

    fun onToggleAutoFetchMetadata(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setAutoFetchMetadata(enabled)
        }
    }

    fun onRescanLibrary() {
        viewModelScope.launch {
            scanDeviceUseCase()
            loadStats()
        }
    }

    fun onFetchMetadata() {
        viewModelScope.launch {
            _uiState.update { it.copy(metadataFetchProgress = "Starting...") }
            fetchMetadataUseCase { current, total ->
                _uiState.update { it.copy(metadataFetchProgress = "$current / $total") }
            }
            _uiState.update { it.copy(metadataFetchProgress = null) }
            loadStats()
        }
    }
}
