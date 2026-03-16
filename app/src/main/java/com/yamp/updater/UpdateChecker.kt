package com.yamp.updater

import android.util.Log
import com.google.gson.Gson
import com.yamp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

sealed class UpdateCheckResult {
    data class UpdateAvailable(val release: GitHubRelease) : UpdateCheckResult()
    data object UpToDate : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

@Singleton
class UpdateChecker @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "UpdateChecker"
        const val GITHUB_OWNER = "praj107"
        const val GITHUB_REPO = "YetAnotherMusicPlayer"
        private const val API_URL =
            "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    }

    private val gson = Gson()

    suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(API_URL)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "YAMP/${BuildConfig.VERSION_NAME}")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext UpdateCheckResult.Error(
                    "GitHub API returned ${response.code}"
                )
            }

            val body = response.body?.string()
                ?: return@withContext UpdateCheckResult.Error("Empty response body")

            val release = gson.fromJson(body, GitHubRelease::class.java)

            if (release.draft || release.prerelease) {
                return@withContext UpdateCheckResult.UpToDate
            }

            if (release.apkAsset == null) {
                return@withContext UpdateCheckResult.Error("Release has no APK asset")
            }

            val currentVersion = BuildConfig.VERSION_NAME
            val remoteVersion = release.version

            Log.d(TAG, "Current: $currentVersion, Remote: $remoteVersion")

            if (VersionComparator.isNewer(currentVersion, remoteVersion)) {
                UpdateCheckResult.UpdateAvailable(release)
            } else {
                UpdateCheckResult.UpToDate
            }
        } catch (e: Exception) {
            Log.e(TAG, "Update check failed", e)
            UpdateCheckResult.Error(e.message ?: "Unknown error")
        }
    }
}
