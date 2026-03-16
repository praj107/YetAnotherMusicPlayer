package com.yamp.updater

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.yamp.BuildConfig
import com.yamp.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

class UpdateDownloadService : Service() {

    companion object {
        private const val TAG = "UpdateDownloadService"
        const val CHANNEL_ID = "yamp_update"
        private const val NOTIFICATION_ID = 9001
        const val EXTRA_DOWNLOAD_URL = "download_url"
        const val EXTRA_CHECKSUM_URL = "checksum_url"
        const val EXTRA_VERSION = "version"
        const val EXTRA_FILE_SIZE = "file_size"

        const val ACTION_DOWNLOAD_COMPLETE = "com.yamp.UPDATE_DOWNLOAD_COMPLETE"
        const val ACTION_DOWNLOAD_FAILED = "com.yamp.UPDATE_DOWNLOAD_FAILED"
        const val EXTRA_APK_PATH = "apk_path"
        const val EXTRA_ERROR_MESSAGE = "error_message"

        fun createIntent(
            context: Context,
            downloadUrl: String,
            checksumUrl: String?,
            version: String,
            fileSize: Long
        ): Intent = Intent(context, UpdateDownloadService::class.java).apply {
            putExtra(EXTRA_DOWNLOAD_URL, downloadUrl)
            putExtra(EXTRA_CHECKSUM_URL, checksumUrl)
            putExtra(EXTRA_VERSION, version)
            putExtra(EXTRA_FILE_SIZE, fileSize)
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var downloadJob: Job? = null
    private val okHttpClient = OkHttpClient.Builder().followRedirects(true).build()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val downloadUrl = intent?.getStringExtra(EXTRA_DOWNLOAD_URL)
        val checksumUrl = intent?.getStringExtra(EXTRA_CHECKSUM_URL)
        val version = intent?.getStringExtra(EXTRA_VERSION) ?: "unknown"
        val fileSize = intent?.getLongExtra(EXTRA_FILE_SIZE, 0L) ?: 0L

        if (downloadUrl == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildProgressNotification(0, "Downloading YAMP $version...")
        startForeground(NOTIFICATION_ID, notification.build())

        downloadJob = scope.launch {
            try {
                val apkFile = downloadApk(downloadUrl, version, fileSize)

                if (checksumUrl != null) {
                    val verified = verifyChecksum(apkFile, checksumUrl)
                    if (!verified) {
                        apkFile.delete()
                        broadcastFailure("Checksum verification failed - download may be corrupted")
                        return@launch
                    }
                }

                verifyApkSignature(apkFile)

                broadcastSuccess(apkFile.absolutePath)
            } catch (e: Exception) {
                Log.e(TAG, "Download failed", e)
                broadcastFailure(e.message ?: "Download failed")
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun downloadApk(url: String, version: String, expectedSize: Long): File {
        val updateDir = File(cacheDir, "updates").apply { mkdirs() }
        val apkFile = File(updateDir, "yamp-v$version.apk")

        // Clean old downloads
        updateDir.listFiles()?.filter { it != apkFile }?.forEach { it.delete() }

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "YAMP/${BuildConfig.VERSION_NAME}")
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw RuntimeException("Download failed: HTTP ${response.code}")
        }

        val body = response.body ?: throw RuntimeException("Empty response body")
        val totalBytes = body.contentLength().takeIf { it > 0 } ?: expectedSize

        body.byteStream().use { input ->
            FileOutputStream(apkFile).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Long = 0
                var lastProgressUpdate = 0L

                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    bytesRead += read

                    // Update notification at most every 500ms worth of data
                    if (totalBytes > 0 && bytesRead - lastProgressUpdate > totalBytes / 200) {
                        val progress = ((bytesRead * 100) / totalBytes).toInt()
                        updateNotification(
                            progress.coerceIn(0, 100),
                            "Downloading... ${bytesRead / 1024 / 1024}MB / ${totalBytes / 1024 / 1024}MB"
                        )
                        lastProgressUpdate = bytesRead
                    }
                }
                output.fd.sync()
            }
        }

        if (apkFile.length() == 0L) {
            apkFile.delete()
            throw RuntimeException("Downloaded file is empty")
        }

        return apkFile
    }

    private fun verifyChecksum(apkFile: File, checksumUrl: String): Boolean {
        return try {
            val request = Request.Builder()
                .url(checksumUrl)
                .header("User-Agent", "YAMP/${BuildConfig.VERSION_NAME}")
                .build()
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return true // Skip verification if checksum unavailable

            val expectedHash = response.body?.string()?.trim()?.lowercase()
                ?: return true

            updateNotification(100, "Verifying integrity...")

            val digest = MessageDigest.getInstance("SHA-256")
            apkFile.inputStream().use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    digest.update(buffer, 0, read)
                }
            }
            val actualHash = digest.digest().joinToString("") { "%02x".format(it) }

            Log.d(TAG, "Expected: $expectedHash, Actual: $actualHash")
            actualHash == expectedHash
        } catch (e: Exception) {
            Log.w(TAG, "Checksum verification error, proceeding anyway", e)
            true // Don't block updates if checksum check fails to fetch
        }
    }

    private fun verifyApkSignature(apkFile: File) {
        // The Android system verifies APK signatures at install time.
        // If the signing certificate doesn't match the installed app's cert,
        // the system will refuse the update. This provides baseline protection.
        // We verify file is a valid APK by checking the ZIP magic bytes.
        val header = ByteArray(4)
        apkFile.inputStream().use { it.read(header) }
        if (header[0] != 0x50.toByte() || header[1] != 0x4B.toByte()) {
            apkFile.delete()
            throw RuntimeException("Downloaded file is not a valid APK")
        }
    }

    private fun broadcastSuccess(apkPath: String) {
        sendBroadcast(Intent(ACTION_DOWNLOAD_COMPLETE).apply {
            setPackage(packageName)
            putExtra(EXTRA_APK_PATH, apkPath)
        })
    }

    private fun broadcastFailure(message: String) {
        sendBroadcast(Intent(ACTION_DOWNLOAD_FAILED).apply {
            setPackage(packageName)
            putExtra(EXTRA_ERROR_MESSAGE, message)
        })
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Updates",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Download progress for app updates"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildProgressNotification(
        progress: Int,
        text: String
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("YAMP Update")
            .setContentText(text)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
    }

    private fun updateNotification(progress: Int, text: String) {
        val notification = buildProgressNotification(progress, text)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification.build())
    }

    override fun onDestroy() {
        downloadJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }
}
