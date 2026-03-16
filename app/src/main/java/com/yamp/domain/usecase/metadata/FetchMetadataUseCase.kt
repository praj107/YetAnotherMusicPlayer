package com.yamp.domain.usecase.metadata

import com.yamp.data.repository.MetadataRepository
import javax.inject.Inject

class FetchMetadataUseCase @Inject constructor(
    private val metadataRepository: MetadataRepository
) {
    suspend operator fun invoke(onProgress: (Int, Int) -> Unit) {
        val incomplete = metadataRepository.getIncompleteMetadataTracks()
        if (incomplete.isNotEmpty()) {
            metadataRepository.batchFetchMetadata(incomplete, onProgress)
        }
    }
}
