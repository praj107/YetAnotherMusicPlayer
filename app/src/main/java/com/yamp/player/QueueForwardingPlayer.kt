package com.yamp.player

import android.annotation.SuppressLint
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

@SuppressLint("UnsafeOptInUsageError")
class QueueForwardingPlayer(
    private val exoPlayer: ExoPlayer,
    private val playbackManager: PlaybackManager
) : ForwardingPlayer(exoPlayer) {

    override fun play() {
        playbackManager.play()
    }

    override fun pause() {
        playbackManager.pause()
    }

    override fun stop() {
        playbackManager.stop()
    }

    override fun getAvailableCommands(): Player.Commands {
        return Player.Commands.Builder()
            .addAllCommands()
            .build()
    }

    override fun isCommandAvailable(command: Int): Boolean {
        return when (command) {
            COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
            COMMAND_SEEK_TO_PREVIOUS -> true
            COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            COMMAND_SEEK_TO_NEXT -> true
            else -> super.isCommandAvailable(command)
        }
    }

    override fun seekToNext() {
        playbackManager.skipNext()
    }

    override fun seekToNextMediaItem() {
        playbackManager.skipNext()
    }

    override fun seekToPrevious() {
        playbackManager.skipPrevious()
    }

    override fun seekToPreviousMediaItem() {
        playbackManager.skipPrevious()
    }
}
