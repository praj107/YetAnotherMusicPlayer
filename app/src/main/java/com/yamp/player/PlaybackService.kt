package com.yamp.player

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ConnectionResult
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.yamp.MainActivity
import com.yamp.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
@SuppressLint("UnsafeOptInUsageError")
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var playbackManager: PlaybackManager

    private var mediaSession: MediaSession? = null

    private val stopCommand = SessionCommand(CUSTOM_ACTION_STOP, Bundle.EMPTY)

    private val stopButton by lazy {
        CommandButton.Builder(CommandButton.ICON_STOP)
            .setDisplayName(getString(R.string.playback_stop))
            .setSessionCommand(stopCommand)
            .setSlots(CommandButton.SLOT_OVERFLOW)
            .build()
    }

    private val sessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ConnectionResult {
            val sessionCommands = ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(stopCommand)
                .build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .setMediaButtonPreferences(listOf(stopButton))
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            return when (customCommand.customAction) {
                CUSTOM_ACTION_STOP -> {
                    playbackManager.stop()
                    stopSelf()
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                else -> Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val player = playbackManager.getOrCreatePlayer()
        val forwardingPlayer = QueueForwardingPlayer(player, playbackManager)
        mediaSession = MediaSession.Builder(this, forwardingPlayer)
            .setSessionActivity(buildSessionActivity())
            .setCallback(sessionCallback)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0 ||
            player.playbackState == Player.STATE_ENDED
        ) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        playbackManager.release()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildSessionActivity(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val CHANNEL_ID = "yamp_playback"
        private const val CUSTOM_ACTION_STOP = "com.yamp.action.STOP_PLAYBACK"
    }
}
