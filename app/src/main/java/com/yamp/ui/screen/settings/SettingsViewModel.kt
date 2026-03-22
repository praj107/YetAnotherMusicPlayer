package com.yamp.ui.screen.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yamp.crash.CrashReporter
import com.yamp.data.repository.MetadataRepository
import com.yamp.data.repository.TrackRepository
import com.yamp.data.repository.UserPreferencesRepository
import com.yamp.domain.usecase.metadata.FetchMetadataUseCase
import com.yamp.domain.usecase.scan.ScanDeviceUseCase
import com.yamp.ui.state.SettingsUiState
import com.yamp.updater.UpdateManager
import com.yamp.updater.UpdateState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val scanDeviceUseCase: ScanDeviceUseCase,
    private val fetchMetadataUseCase: FetchMetadataUseCase,
    private val trackRepository: TrackRepository,
    private val metadataRepository: MetadataRepository,
    private val updateManager: UpdateManager,
    private val crashReporter: CrashReporter,
    private val userPreferences: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val updateState: StateFlow<UpdateState> = updateManager.state

    init {
        viewModelScope.launch {
            userPreferences.autoFetchMetadata.collect { enabled ->
                _uiState.update { it.copy(autoFetchMetadata = enabled) }
            }
        }
        viewModelScope.launch {
            updateManager.state.collect { state ->
                _uiState.update { it.copy(updateState = state) }
            }
        }
        viewModelScope.launch {
            crashReporter.pendingReport.collect { report ->
                _uiState.update { it.copy(pendingCrashReport = report) }
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
            if (userPreferences.autoFetchMetadata.first()) {
                fetchMetadataUseCase { _, _ -> }
            }
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

    fun onCheckForUpdate() {
        viewModelScope.launch {
            updateManager.checkForUpdate()
        }
    }

    fun onDownloadUpdate() {
        val state = updateManager.state.value
        if (state is UpdateState.Available) {
            updateManager.startDownload(state.release)
        }
    }

    fun onInstallUpdate() {
        val state = updateManager.state.value
        if (state is UpdateState.ReadyToInstall) {
            updateManager.installUpdate(state.apkPath)
        }
    }

    fun onDismissUpdate() {
        updateManager.dismiss()
    }

    fun onDismissCrashReport() {
        crashReporter.dismissPendingReport()
    }

    fun getCrashIssueIntent(report: com.yamp.crash.PendingCrashReport): Intent =
        crashReporter.buildIssueIntent(report)

    fun getCrashShareIntent(report: com.yamp.crash.PendingCrashReport): Intent? =
        crashReporter.buildShareIntent(report)

    fun canInstallPackages(): Boolean = updateManager.canInstallPackages()

    fun getInstallPermissionIntent(): Intent = updateManager.getInstallPermissionIntent()
}
