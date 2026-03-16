package com.yamp.domain.usecase.sort

import com.yamp.data.repository.TrackRepository
import com.yamp.domain.model.SortOrder
import com.yamp.domain.model.Track
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SortTracksUseCase @Inject constructor(
    private val trackRepository: TrackRepository
) {
    operator fun invoke(sortOrder: SortOrder): Flow<List<Track>> =
        trackRepository.getTracksSorted(sortOrder)
}
