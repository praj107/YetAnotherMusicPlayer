package com.yamp.player

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.yamp.domain.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

enum class RepeatMode { OFF, ONE, ALL }

@Singleton
class PlaybackManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    val queueManager = QueueManager()
    private var _repeatMode = RepeatMode.OFF
    val repeatMode: RepeatMode get() = _repeatMode

    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var exoPlayer: ExoPlayer? = null
    private var positionUpdateJob: Job? = null
    private var fadeJob: Job? = null
    private var outputMonitorRegistered = false

    private var onTrackCompleted: ((Long, Long) -> Unit)? = null
    private var onTrackStarted: ((Long) -> Unit)? = null
    private var trackStartTime: Long = 0L

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            if (removedDevices.any(::isExternalPlaybackDevice) && exoPlayer?.isPlaying == true) {
                pause()
            }
        }
    }

    fun setCallbacks(
        onCompleted: (trackId: Long, durationListened: Long) -> Unit,
        onStarted: (trackId: Long) -> Unit
    ) {
        onTrackCompleted = onCompleted
        onTrackStarted = onStarted
    }

    fun getOrCreatePlayer(): ExoPlayer {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    true
                )
                setHandleAudioBecomingNoisy(true)
                addListener(playerListener)
            }
            registerOutputMonitor()
        }
        return exoPlayer!!
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_ENDED -> handleTrackEnded()
                Player.STATE_BUFFERING -> {
                    queueManager.currentTrack?.let {
                        _playbackState.value = PlaybackState.Buffering(it)
                    }
                }
                Player.STATE_READY -> updatePlayingState()
                Player.STATE_IDLE -> {
                    if (queueManager.currentTrack == null) {
                        _playbackState.value = PlaybackState.Idle
                    }
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlayingState()
        }
    }

    fun playTrack(track: Track, queue: List<Track>? = null, startIndex: Int = 0) {
        if (queue != null) {
            queueManager.setQueue(queue, startIndex)
        }

        ensurePlaybackService()
        val player = getOrCreatePlayer()
        cancelFade(resetVolume = true)

        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(track.contentUri))
            .setMediaId(track.id.toString())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .setArtworkUri(track.albumArtUri?.let { Uri.parse(it) })
                    .build()
            )
            .build()

        player.setMediaItem(mediaItem)
        player.prepare()
        trackStartTime = System.currentTimeMillis()
        onTrackStarted?.invoke(track.id)
        fadeInAndRun(player) { player.play() }
    }

    fun play() {
        ensurePlaybackService()
        val player = getOrCreatePlayer()
        fadeInAndRun(player) { player.play() }
    }

    fun pause() {
        val player = exoPlayer ?: return
        fadeOutAndRun(player) {
            player.pause()
            restoreDefaultVolume(player)
            stopPositionUpdates()
            updatePlayingState()
        }
    }

    fun skipNext() {
        val next = queueManager.skipToNext()
        if (next != null) {
            reportTrackCompletion(false)
            playTrack(next)
        }
    }

    fun skipPrevious() {
        val player = exoPlayer
        if (player != null && player.currentPosition > 3000) {
            player.seekTo(0)
        } else {
            val prev = queueManager.skipToPrevious()
            if (prev != null) {
                reportTrackCompletion(false)
                playTrack(prev)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    fun toggleRepeat(): RepeatMode {
        _repeatMode = when (_repeatMode) {
            RepeatMode.OFF -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.OFF
        }
        exoPlayer?.repeatMode = when (_repeatMode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
        return _repeatMode
    }

    fun toggleShuffle(): Boolean = queueManager.toggleShuffle()

    fun stop() {
        reportTrackCompletion(false)
        val player = exoPlayer ?: run {
            queueManager.clear()
            _playbackState.value = PlaybackState.Idle
            return
        }

        fadeOutAndRun(player) {
            player.stop()
            player.clearMediaItems()
            restoreDefaultVolume(player)
            queueManager.clear()
            stopPositionUpdates()
            _playbackState.value = PlaybackState.Idle
        }
    }

    fun release() {
        cancelFade(resetVolume = false)
        stopPositionUpdates()
        unregisterOutputMonitor()
        exoPlayer?.removeListener(playerListener)
        exoPlayer?.release()
        exoPlayer = null
        queueManager.clear()
        _playbackState.value = PlaybackState.Idle
    }

    private fun handleTrackEnded() {
        reportTrackCompletion(true)
        when (_repeatMode) {
            RepeatMode.ONE -> {
                exoPlayer?.seekTo(0)
                exoPlayer?.play()
            }
            RepeatMode.ALL, RepeatMode.OFF -> {
                if (queueManager.hasNext || _repeatMode == RepeatMode.ALL) {
                    val next = queueManager.skipToNext()
                    if (next != null) {
                        playTrack(next)
                    }
                } else {
                    queueManager.clear()
                    _playbackState.value = PlaybackState.Idle
                    stopPositionUpdates()
                }
            }
        }
    }

    private fun reportTrackCompletion(completed: Boolean) {
        val track = queueManager.currentTrack ?: return
        val duration = System.currentTimeMillis() - trackStartTime
        if (completed || duration > 0L) {
            onTrackCompleted?.invoke(track.id, duration)
        }
    }

    private fun updatePlayingState() {
        val player = exoPlayer ?: run {
            _playbackState.value = PlaybackState.Idle
            return
        }
        val track = queueManager.currentTrack ?: run {
            if (player.mediaItemCount == 0) {
                _playbackState.value = PlaybackState.Idle
            }
            return
        }

        _playbackState.value = if (player.isPlaying) {
            PlaybackState.Playing(
                track = track,
                positionMs = player.currentPosition,
                durationMs = player.duration.coerceAtLeast(0),
                queue = queueManager.queue,
                queueIndex = queueManager.currentIndex
            )
        } else if (player.playbackState == Player.STATE_READY) {
            PlaybackState.Paused(
                track = track,
                positionMs = player.currentPosition,
                durationMs = player.duration.coerceAtLeast(0),
                queue = queueManager.queue,
                queueIndex = queueManager.currentIndex
            )
        } else {
            _playbackState.value
        }
    }

    private fun startPositionUpdates() {
        stopPositionUpdates()
        positionUpdateJob = scope.launch {
            while (isActive) {
                updatePlayingState()
                delay(500L)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    private fun fadeInAndRun(player: ExoPlayer, onStart: () -> Unit) {
        cancelFade(resetVolume = false)
        fadeJob = scope.launch {
            try {
                player.volume = 0f
                onStart()
                startPositionUpdates()
                repeat(VOLUME_FADE_STEPS) { step ->
                    player.volume = (step + 1) / VOLUME_FADE_STEPS.toFloat()
                    delay(VOLUME_FADE_STEP_DELAY_MS)
                }
                restoreDefaultVolume(player)
                updatePlayingState()
            } finally {
                fadeJob = null
            }
        }
    }

    private fun fadeOutAndRun(player: ExoPlayer, onComplete: () -> Unit) {
        cancelFade(resetVolume = false)
        if (!player.isPlaying && !player.playWhenReady) {
            onComplete()
            return
        }

        fadeJob = scope.launch {
            try {
                val startVolume = player.volume.coerceIn(0f, DEFAULT_VOLUME)
                repeat(VOLUME_FADE_STEPS) { step ->
                    val remaining = 1f - ((step + 1) / VOLUME_FADE_STEPS.toFloat())
                    player.volume = startVolume * remaining
                    delay(VOLUME_FADE_STEP_DELAY_MS)
                }
                onComplete()
            } finally {
                fadeJob = null
            }
        }
    }

    private fun cancelFade(resetVolume: Boolean = true) {
        fadeJob?.cancel()
        fadeJob = null
        if (resetVolume) {
            exoPlayer?.let(::restoreDefaultVolume)
        }
    }

    private fun restoreDefaultVolume(player: ExoPlayer) {
        player.volume = DEFAULT_VOLUME
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun ensurePlaybackService() {
        val intent = Intent(context, PlaybackService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    private fun registerOutputMonitor() {
        if (outputMonitorRegistered) return
        audioManager?.registerAudioDeviceCallback(audioDeviceCallback, null)
        outputMonitorRegistered = true
    }

    private fun unregisterOutputMonitor() {
        if (!outputMonitorRegistered) return
        audioManager?.unregisterAudioDeviceCallback(audioDeviceCallback)
        outputMonitorRegistered = false
    }

    private fun isExternalPlaybackDevice(device: AudioDeviceInfo): Boolean {
        return when (device.type) {
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_SPEAKER,
            AudioDeviceInfo.TYPE_HEARING_AID,
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_LINE_ANALOG,
            AudioDeviceInfo.TYPE_LINE_DIGITAL,
            AudioDeviceInfo.TYPE_AUX_LINE -> true
            else -> false
        }
    }

    companion object {
        private const val DEFAULT_VOLUME = 1f
        private const val VOLUME_FADE_STEPS = 8
        private const val VOLUME_FADE_STEP_DELAY_MS = 18L
    }
}
