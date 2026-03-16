package com.yamp.domain.usecase.playlist

import com.yamp.data.repository.PlaylistRepository
import com.yamp.domain.model.Playlist
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPlaylistsUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    operator fun invoke(): Flow<List<Playlist>> =
        playlistRepository.getAllPlaylists()
}
