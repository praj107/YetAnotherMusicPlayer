package com.yamp.domain.ontology

import com.yamp.domain.model.Track

class OntologyBuilder {

    fun build(tracks: List<Track>): MusicOntology {
        val nodes = mutableMapOf<String, OntologyNode>()
        val edges = mutableListOf<OntologyEdge>()

        for (track in tracks) {
            val trackNode = OntologyNode.TrackNode(track.id, track.title)
            nodes[trackNode.id] = trackNode

            // Artist
            val artistNode = OntologyNode.ArtistNode(track.artist)
            nodes[artistNode.id] = artistNode
            edges.add(OntologyEdge(trackNode, OntologyRelation.PERFORMED_BY, artistNode))

            // Album
            val albumNode = OntologyNode.AlbumNode(track.album, track.artist)
            nodes[albumNode.id] = albumNode
            edges.add(OntologyEdge(trackNode, OntologyRelation.BELONGS_TO_ALBUM, albumNode))
            edges.add(OntologyEdge(albumNode, OntologyRelation.ALBUM_BY, artistNode))

            // Genre
            if (track.genre != null) {
                val genreNode = OntologyNode.GenreNode(track.genre)
                nodes[genreNode.id] = genreNode
                edges.add(OntologyEdge(trackNode, OntologyRelation.HAS_GENRE, genreNode))
                edges.add(OntologyEdge(artistNode, OntologyRelation.HAS_GENRE, genreNode))
                edges.add(OntologyEdge(albumNode, OntologyRelation.HAS_GENRE, genreNode))
            }

            // Folder
            val folderNode = OntologyNode.FolderNode(track.folderPath)
            nodes[folderNode.id] = folderNode
            edges.add(OntologyEdge(trackNode, OntologyRelation.LOCATED_IN, folderNode))
        }

        // Derive SIMILAR_TO between artists who share genres
        val artistGenres = mutableMapOf<String, MutableSet<String>>()
        for (edge in edges) {
            if (edge.relation == OntologyRelation.HAS_GENRE && edge.source is OntologyNode.ArtistNode) {
                artistGenres.getOrPut(edge.source.id) { mutableSetOf() }
                    .add(edge.target.id)
            }
        }
        val artistIds = artistGenres.keys.toList()
        for (i in artistIds.indices) {
            for (j in i + 1 until artistIds.size) {
                val genresA = artistGenres[artistIds[i]] ?: continue
                val genresB = artistGenres[artistIds[j]] ?: continue
                if (genresA.intersect(genresB).isNotEmpty()) {
                    val nodeA = nodes[artistIds[i]] ?: continue
                    val nodeB = nodes[artistIds[j]] ?: continue
                    edges.add(OntologyEdge(nodeA, OntologyRelation.SIMILAR_TO, nodeB))
                }
            }
        }

        return MusicOntology(nodes = nodes, edges = edges.distinct())
    }
}
