package com.yamp.domain.usecase.search

import com.yamp.data.repository.TrackRepository
import com.yamp.domain.model.Track
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchTracksUseCase @Inject constructor(
    private val trackRepository: TrackRepository
) {
    operator fun invoke(query: String): Flow<List<Track>> =
        trackRepository.searchTracks(query)
}
