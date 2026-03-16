package com.yamp.domain.usecase.playback

import com.yamp.player.PlaybackManager
import javax.inject.Inject

class PauseUseCase @Inject constructor(
    private val playbackManager: PlaybackManager
) {
    operator fun invoke() = playbackManager.pause()
}
