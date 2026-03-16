package com.yamp.domain.ontology

sealed class OntologyNode(val id: String, val label: String) {
    data class ArtistNode(val name: String) : OntologyNode("artist:$name", name)
    data class AlbumNode(val name: String, val artist: String) :
        OntologyNode("album:$artist:$name", name)
    data class GenreNode(val name: String) : OntologyNode("genre:$name", name)
    data class TrackNode(val trackId: Long, val title: String) :
        OntologyNode("track:$trackId", title)
    data class FolderNode(val path: String) :
        OntologyNode("folder:$path", path.substringAfterLast('/'))
}
