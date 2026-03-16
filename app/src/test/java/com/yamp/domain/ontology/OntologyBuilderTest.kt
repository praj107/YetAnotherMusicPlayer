package com.yamp.domain.ontology

import com.google.common.truth.Truth.assertThat
import com.yamp.domain.model.Track
import org.junit.Test

class OntologyBuilderTest {

    private val builder = OntologyBuilder()

    @Test
    fun `builds track nodes for each track`() {
        val tracks = listOf(createTrack(1), createTrack(2))
        val ontology = builder.build(tracks)
        assertThat(ontology.nodes).containsKey("track:1")
        assertThat(ontology.nodes).containsKey("track:2")
    }

    @Test
    fun `builds artist nodes`() {
        val tracks = listOf(createTrack(1, artist = "The Beatles"))
        val ontology = builder.build(tracks)
        assertThat(ontology.nodes).containsKey("artist:The Beatles")
    }

    @Test
    fun `shared artist creates single node`() {
        val tracks = listOf(
            createTrack(1, artist = "The Beatles"),
            createTrack(2, artist = "The Beatles")
        )
        val ontology = builder.build(tracks)
        val artistNodes = ontology.nodes.values.filterIsInstance<OntologyNode.ArtistNode>()
        assertThat(artistNodes.filter { it.name == "The Beatles" }).hasSize(1)
    }

    @Test
    fun `creates genre edges`() {
        val tracks = listOf(createTrack(1, genre = "Rock"))
        val ontology = builder.build(tracks)
        val genreEdges = ontology.edges.filter { it.relation == OntologyRelation.HAS_GENRE }
        assertThat(genreEdges).isNotEmpty()
    }

    @Test
    fun `creates SIMILAR_TO edges for artists sharing genre`() {
        val tracks = listOf(
            createTrack(1, artist = "Artist A", genre = "Rock"),
            createTrack(2, artist = "Artist B", genre = "Rock")
        )
        val ontology = builder.build(tracks)
        val similarEdges = ontology.edges.filter { it.relation == OntologyRelation.SIMILAR_TO }
        assertThat(similarEdges).isNotEmpty()
    }

    @Test
    fun `no SIMILAR_TO when artists have different genres`() {
        val tracks = listOf(
            createTrack(1, artist = "Artist A", genre = "Rock"),
            createTrack(2, artist = "Artist B", genre = "Jazz")
        )
        val ontology = builder.build(tracks)
        val similarEdges = ontology.edges.filter { it.relation == OntologyRelation.SIMILAR_TO }
        assertThat(similarEdges).isEmpty()
    }

    @Test
    fun `relatedTracks finds tracks via shared artist`() {
        val tracks = listOf(
            createTrack(1, artist = "Shared Artist"),
            createTrack(2, artist = "Shared Artist"),
            createTrack(3, artist = "Other Artist")
        )
        val ontology = builder.build(tracks)
        val related = ontology.relatedTracks(1)
        assertThat(related.map { it.trackId }).contains(2L)
    }

    @Test
    fun `empty track list builds empty ontology`() {
        val ontology = builder.build(emptyList())
        assertThat(ontology.nodes).isEmpty()
        assertThat(ontology.edges).isEmpty()
    }

    private fun createTrack(
        id: Long,
        artist: String = "Artist",
        genre: String? = "Rock"
    ) = Track(
        id = id, contentUri = "content://media/$id", title = "Track $id",
        artist = artist, album = "Album", albumArtUri = null, genre = genre,
        duration = 200000, trackNumber = null, year = null, mimeType = "audio/mpeg",
        folderPath = "/Music", metadataComplete = true
    )
}
