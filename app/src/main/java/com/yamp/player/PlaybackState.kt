package com.yamp.player

import com.yamp.domain.model.Track

sealed class PlaybackState {
    data object Idle : PlaybackState()

    data class Playing(
        val track: Track,
        val positionMs: Long,
        val durationMs: Long,
        val queue: List<Track>,
        val queueIndex: Int
    ) : PlaybackState()

    data class Paused(
        val track: Track,
        val positionMs: Long,
        val durationMs: Long,
        val queue: List<Track>,
        val queueIndex: Int
    ) : PlaybackState()

    data class Buffering(val track: Track) : PlaybackState()

    data class Error(val message: String, val track: Track?) : PlaybackState()
}

val PlaybackState.currentTrack: Track?
    get() = when (this) {
        is PlaybackState.Playing -> track
        is PlaybackState.Paused -> track
        is PlaybackState.Buffering -> track
        is PlaybackState.Error -> track
        is PlaybackState.Idle -> null
    }

val PlaybackState.isPlaying: Boolean
    get() = this is PlaybackState.Playing
