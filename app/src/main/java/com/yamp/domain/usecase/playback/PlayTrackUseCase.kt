package com.yamp.domain.usecase.playback

import com.yamp.domain.model.Track
import com.yamp.player.PlaybackManager
import javax.inject.Inject

class PlayTrackUseCase @Inject constructor(
    private val playbackManager: PlaybackManager
) {
    operator fun invoke(track: Track, queue: List<Track>? = null, startIndex: Int = 0) {
        playbackManager.playTrack(track, queue, startIndex)
    }
}
