package com.yamp.domain.ontology

class MusicOntology(
    val nodes: Map<String, OntologyNode>,
    val edges: List<OntologyEdge>
) {
    private val adjacencyList: Map<String, List<Pair<OntologyRelation, OntologyNode>>> by lazy {
        edges.groupBy { it.source.id }
            .mapValues { (_, edges) -> edges.map { it.relation to it.target } }
    }

    private val reverseAdjacencyList: Map<String, List<Pair<OntologyRelation, OntologyNode>>> by lazy {
        edges.groupBy { it.target.id }
            .mapValues { (_, edges) -> edges.map { it.relation to it.source } }
    }

    fun neighborsOf(nodeId: String, relation: OntologyRelation? = null): List<OntologyNode> {
        val forward = adjacencyList[nodeId]?.let { neighbors ->
            if (relation != null) neighbors.filter { it.first == relation } else neighbors
        }?.map { it.second } ?: emptyList()

        val reverse = reverseAdjacencyList[nodeId]?.let { neighbors ->
            if (relation != null) neighbors.filter { it.first == relation } else neighbors
        }?.map { it.second } ?: emptyList()

        return (forward + reverse).distinctBy { it.id }
    }

    fun relatedTracks(trackId: Long, maxDepth: Int = 2): List<OntologyNode.TrackNode> {
        val startId = "track:$trackId"
        if (startId !in nodes) return emptyList()

        val visited = mutableSetOf(startId)
        val result = mutableListOf<OntologyNode.TrackNode>()
        var frontier = listOf(startId)

        repeat(maxDepth) {
            val nextFrontier = mutableListOf<String>()
            for (nodeId in frontier) {
                for (neighbor in neighborsOf(nodeId)) {
                    if (neighbor.id !in visited) {
                        visited.add(neighbor.id)
                        nextFrontier.add(neighbor.id)
                        if (neighbor is OntologyNode.TrackNode && neighbor.trackId != trackId) {
                            result.add(neighbor)
                        }
                    }
                }
            }
            frontier = nextFrontier
        }

        return result
    }
}
