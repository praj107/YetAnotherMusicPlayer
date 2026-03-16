package com.yamp.domain.usecase.playback

import com.yamp.player.PlaybackManager
import javax.inject.Inject

class SeekToUseCase @Inject constructor(
    private val playbackManager: PlaybackManager
) {
    operator fun invoke(positionMs: Long) = playbackManager.seekTo(positionMs)
}
