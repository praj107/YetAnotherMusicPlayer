package com.yamp.updater

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()
    data class Available(val release: GitHubRelease) : UpdateState()
    data object Downloading : UpdateState()
    data class ReadyToInstall(val apkPath: String, val release: GitHubRelease) : UpdateState()
    data class Failed(val message: String) : UpdateState()
    data object UpToDate : UpdateState()
}

@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val updateChecker: UpdateChecker,
    private val updateInstaller: UpdateInstaller
) {
    companion object {
        private const val TAG = "UpdateManager"
    }

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private var pendingRelease: GitHubRelease? = null

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                UpdateDownloadService.ACTION_DOWNLOAD_COMPLETE -> {
                    val apkPath = intent.getStringExtra(UpdateDownloadService.EXTRA_APK_PATH)
                    if (apkPath != null && pendingRelease != null) {
                        _state.value = UpdateState.ReadyToInstall(apkPath, pendingRelease!!)
                    }
                }
                UpdateDownloadService.ACTION_DOWNLOAD_FAILED -> {
                    val error = intent.getStringExtra(UpdateDownloadService.EXTRA_ERROR_MESSAGE)
                        ?: "Download failed"
                    _state.value = UpdateState.Failed(error)
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(UpdateDownloadService.ACTION_DOWNLOAD_COMPLETE)
            addAction(UpdateDownloadService.ACTION_DOWNLOAD_FAILED)
        }
        ContextCompat.registerReceiver(
            context,
            downloadReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // Clean up old downloads on startup
        updateInstaller.cleanupOldUpdates(context)
    }

    suspend fun checkForUpdate() {
        _state.value = UpdateState.Checking
        when (val result = updateChecker.checkForUpdate()) {
            is UpdateCheckResult.UpdateAvailable -> {
                _state.value = UpdateState.Available(result.release)
            }
            is UpdateCheckResult.UpToDate -> {
                _state.value = UpdateState.UpToDate
            }
            is UpdateCheckResult.Error -> {
                Log.w(TAG, "Update check failed: ${result.message}")
                _state.value = UpdateState.Failed(result.message)
            }
        }
    }

    fun startDownload(release: GitHubRelease) {
        val apkAsset = release.apkAsset ?: return
        pendingRelease = release

        _state.value = UpdateState.Downloading

        val intent = UpdateDownloadService.createIntent(
            context = context,
            downloadUrl = apkAsset.browserDownloadUrl,
            checksumUrl = release.checksumAsset?.browserDownloadUrl,
            version = release.version,
            fileSize = apkAsset.size
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun installUpdate(apkPath: String) {
        if (!updateInstaller.canInstallPackages(context)) {
            // Caller should handle this by launching permission settings
            return
        }
        updateInstaller.installApk(context, apkPath)
    }

    fun getInstallPermissionIntent(): Intent =
        updateInstaller.getInstallPermissionIntent(context)

    fun canInstallPackages(): Boolean =
        updateInstaller.canInstallPackages(context)

    fun dismiss() {
        _state.value = UpdateState.Idle
    }
}
