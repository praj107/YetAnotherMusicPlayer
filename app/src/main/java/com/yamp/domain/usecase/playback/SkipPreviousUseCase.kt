package com.yamp.domain.usecase.playback

import com.yamp.player.PlaybackManager
import javax.inject.Inject

class SkipPreviousUseCase @Inject constructor(
    private val playbackManager: PlaybackManager
) {
    operator fun invoke() = playbackManager.skipPrevious()
}
