package com.yamp.domain.usecase.ontology

import com.yamp.data.repository.TrackRepository
import com.yamp.domain.ontology.MusicOntology
import com.yamp.domain.ontology.OntologyBuilder
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class BuildOntologyUseCase @Inject constructor(
    private val trackRepository: TrackRepository,
    private val ontologyBuilder: OntologyBuilder
) {
    suspend operator fun invoke(): MusicOntology {
        val tracks = trackRepository.getAllTracks().first()
        return ontologyBuilder.build(tracks)
    }
}
