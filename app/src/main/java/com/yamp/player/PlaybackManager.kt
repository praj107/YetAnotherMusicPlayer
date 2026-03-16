package com.yamp.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.yamp.domain.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

    private var exoPlayer: ExoPlayer? = null
    private var positionUpdateJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private var onTrackCompleted: ((Long, Long) -> Unit)? = null
    private var onTrackStarted: ((Long) -> Unit)? = null

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
                addListener(playerListener)
            }
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
                else -> {}
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlayingState()
        }
    }

    private var trackStartTime: Long = 0L

    fun playTrack(track: Track, queue: List<Track>? = null, startIndex: Int = 0) {
        if (queue != null) {
            queueManager.setQueue(queue, startIndex)
        }
        val player = getOrCreatePlayer()
        val mediaItem = MediaItem.fromUri(Uri.parse(track.contentUri))
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
        trackStartTime = System.currentTimeMillis()
        onTrackStarted?.invoke(track.id)
        startPositionUpdates()
    }

    fun play() {
        exoPlayer?.play()
        startPositionUpdates()
    }

    fun pause() {
        exoPlayer?.pause()
        stopPositionUpdates()
        updatePlayingState()
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
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        stopPositionUpdates()
        _playbackState.value = PlaybackState.Idle
    }

    fun release() {
        stop()
        exoPlayer?.removeListener(playerListener)
        exoPlayer?.release()
        exoPlayer = null
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
                    skipNext()
                } else {
                    _playbackState.value = PlaybackState.Idle
                    stopPositionUpdates()
                }
            }
        }
    }

    private fun reportTrackCompletion(completed: Boolean) {
        val track = queueManager.currentTrack ?: return
        val duration = System.currentTimeMillis() - trackStartTime
        onTrackCompleted?.invoke(track.id, duration)
    }

    private fun updatePlayingState() {
        val player = exoPlayer ?: return
        val track = queueManager.currentTrack ?: return
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
}
