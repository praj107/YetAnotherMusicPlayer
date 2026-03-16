package com.yamp.domain.ontology

enum class OntologyRelation {
    PERFORMED_BY,
    BELONGS_TO_ALBUM,
    HAS_GENRE,
    LOCATED_IN,
    ALBUM_BY,
    SIMILAR_TO,
    FREQUENTLY_FOLLOWED
}

data class OntologyEdge(
    val source: OntologyNode,
    val relation: OntologyRelation,
    val target: OntologyNode
)
