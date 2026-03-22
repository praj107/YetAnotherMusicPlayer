package com.yamp.ui.screen.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yamp.data.repository.LibraryCollectionOrderRepository
import com.yamp.data.repository.TrackRepository
import com.yamp.domain.media.pickRepresentativeArtwork
import com.yamp.domain.model.Album
import com.yamp.domain.model.Artist
import com.yamp.domain.model.Genre
import com.yamp.domain.model.LibraryCollectionType
import com.yamp.domain.model.SortDirection
import com.yamp.domain.model.SortField
import com.yamp.domain.model.SortOrder
import com.yamp.domain.model.Track
import com.yamp.ui.state.LibraryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val trackRepository: TrackRepository,
    private val orderRepository: LibraryCollectionOrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState(isLoading = true))
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private var tracksJob: Job? = null
    private var allTracks: List<Track> = emptyList()
    private var albumOrder: List<String> = emptyList()
    private var artistOrder: List<String> = emptyList()
    private var genreOrder: List<String> = emptyList()

    init {
        loadTracks()
        observeAllTracks()
        observeCollectionOrders()
    }

    private fun loadTracks() {
        tracksJob?.cancel()
        tracksJob = viewModelScope.launch {
            trackRepository.getTracksSorted(_uiState.value.currentSort).collect { tracks ->
                _uiState.update { it.copy(tracks = tracks, isLoading = false) }
            }
        }
    }

    private fun observeAllTracks() {
        viewModelScope.launch {
            trackRepository.getAllTracks().collect { tracks ->
                allTracks = tracks
                rebuildCollections()
            }
        }
    }

    private fun observeCollectionOrders() {
        viewModelScope.launch {
            orderRepository.observeTabOrder(LibraryCollectionType.ALBUMS).collect { order ->
                albumOrder = order
                rebuildCollections()
            }
        }
        viewModelScope.launch {
            orderRepository.observeTabOrder(LibraryCollectionType.ARTISTS).collect { order ->
                artistOrder = order
                rebuildCollections()
            }
        }
        viewModelScope.launch {
            orderRepository.observeTabOrder(LibraryCollectionType.GENRES).collect { order ->
                genreOrder = order
                rebuildCollections()
            }
        }
    }

    private fun rebuildCollections() {
        val albums = applyOrder(
            items = allTracks
                .groupBy { it.album }
                .map { (albumName, albumTracks) ->
                    Album(
                        name = albumName,
                        artist = albumTracks.firstOrNull()?.artist ?: "Unknown Artist",
                        albumArtUri = pickRepresentativeArtwork(albumTracks),
                        year = albumTracks.firstOrNull()?.year,
                        tracks = albumTracks.sortedWith(compareBy({ it.trackNumber ?: Int.MAX_VALUE }, { it.title }))
                    )
                },
            order = albumOrder,
            keySelector = { it.key },
            fallback = compareBy<Album>({ it.name.lowercase() }, { it.artist.lowercase() })
        )

        val artists = applyOrder(
            items = allTracks
                .groupBy { it.artist }
                .map { (artistName, artistTracks) ->
                    Artist(
                        name = artistName,
                        trackCount = artistTracks.size,
                        albumCount = artistTracks.map { it.album }.distinct().size,
                        artworkUri = pickRepresentativeArtwork(artistTracks),
                        tracks = artistTracks.sortedBy { it.title.lowercase() }
                    )
                },
            order = artistOrder,
            keySelector = { it.key },
            fallback = compareBy<Artist> { it.name.lowercase() }
        )

        val genres = applyOrder(
            items = allTracks
                .filter { !it.genre.isNullOrBlank() }
                .groupBy { it.genre.orEmpty() }
                .map { (genreName, genreTracks) ->
                    Genre(
                        name = genreName,
                        trackCount = genreTracks.size,
                        artworkUri = pickRepresentativeArtwork(genreTracks),
                        tracks = genreTracks.sortedBy { it.title.lowercase() }
                    )
                },
            order = genreOrder,
            keySelector = { it.key },
            fallback = compareBy<Genre> { it.name.lowercase() }
        )

        val selectedCollection = refreshSelectedCollection(
            current = _uiState.value.selectedCollection,
            albums = albums,
            artists = artists,
            genres = genres
        )

        _uiState.update {
            it.copy(
                albums = albums,
                artists = artists,
                genres = genres,
                selectedCollection = selectedCollection,
                isLoading = false
            )
        }
    }

    fun onSortFieldSelected(field: SortField) {
        val currentSort = _uiState.value.currentSort
        val newDirection = if (currentSort.field == field) {
            if (currentSort.direction == SortDirection.ASCENDING) SortDirection.DESCENDING
            else SortDirection.ASCENDING
        } else {
            SortDirection.ASCENDING
        }

        _uiState.update { it.copy(currentSort = SortOrder(field, newDirection)) }
        loadTracks()
    }

    fun onTabSelected(tab: Int) {
        _uiState.update { it.copy(selectedTab = tab, selectedCollection = null) }
    }

    fun onAlbumSelected(album: Album) {
        _uiState.update {
            it.copy(
                selectedCollection = LibraryCollectionDetail(
                    type = LibraryCollectionType.ALBUMS,
                    title = album.name,
                    subtitle = "${album.artist} • ${album.tracks.size} tracks",
                    artworkUri = album.albumArtUri,
                    tracks = album.tracks
                )
            )
        }
    }

    fun onArtistSelected(artist: Artist) {
        _uiState.update {
            it.copy(
                selectedCollection = LibraryCollectionDetail(
                    type = LibraryCollectionType.ARTISTS,
                    title = artist.name,
                    subtitle = "${artist.trackCount} tracks • ${artist.albumCount} albums",
                    artworkUri = artist.artworkUri,
                    tracks = artist.tracks
                )
            )
        }
    }

    fun onGenreSelected(genre: Genre) {
        _uiState.update {
            it.copy(
                selectedCollection = LibraryCollectionDetail(
                    type = LibraryCollectionType.GENRES,
                    title = genre.name,
                    subtitle = "${genre.trackCount} tracks",
                    artworkUri = genre.artworkUri,
                    tracks = genre.tracks
                )
            )
        }
    }

    fun onCollectionBack() {
        _uiState.update { it.copy(selectedCollection = null) }
    }

    fun onAlbumMove(fromIndex: Int, toIndex: Int) {
        val reordered = _uiState.value.albums.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
        _uiState.update { it.copy(albums = reordered) }
        persistOrder(LibraryCollectionType.ALBUMS, reordered.map { it.key })
    }

    fun onArtistMove(fromIndex: Int, toIndex: Int) {
        val reordered = _uiState.value.artists.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
        _uiState.update { it.copy(artists = reordered) }
        persistOrder(LibraryCollectionType.ARTISTS, reordered.map { it.key })
    }

    fun onGenreMove(fromIndex: Int, toIndex: Int) {
        val reordered = _uiState.value.genres.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
        _uiState.update { it.copy(genres = reordered) }
        persistOrder(LibraryCollectionType.GENRES, reordered.map { it.key })
    }

    private fun persistOrder(type: LibraryCollectionType, itemKeys: List<String>) {
        viewModelScope.launch {
            orderRepository.saveTabOrder(type, itemKeys)
        }
    }

    private fun refreshSelectedCollection(
        current: LibraryCollectionDetail?,
        albums: List<Album>,
        artists: List<Artist>,
        genres: List<Genre>
    ): LibraryCollectionDetail? {
        current ?: return null

        return when (current.type) {
            LibraryCollectionType.ALBUMS -> albums.firstOrNull { it.name == current.title }?.let { album ->
                LibraryCollectionDetail(
                    type = current.type,
                    title = album.name,
                    subtitle = "${album.artist} • ${album.tracks.size} tracks",
                    artworkUri = album.albumArtUri,
                    tracks = album.tracks
                )
            }
            LibraryCollectionType.ARTISTS -> artists.firstOrNull { it.name == current.title }?.let { artist ->
                LibraryCollectionDetail(
                    type = current.type,
                    title = artist.name,
                    subtitle = "${artist.trackCount} tracks • ${artist.albumCount} albums",
                    artworkUri = artist.artworkUri,
                    tracks = artist.tracks
                )
            }
            LibraryCollectionType.GENRES -> genres.firstOrNull { it.name == current.title }?.let { genre ->
                LibraryCollectionDetail(
                    type = current.type,
                    title = genre.name,
                    subtitle = "${genre.trackCount} tracks",
                    artworkUri = genre.artworkUri,
                    tracks = genre.tracks
                )
            }
        }
    }

    private fun <T> applyOrder(
        items: List<T>,
        order: List<String>,
        keySelector: (T) -> String,
        fallback: Comparator<T>
    ): List<T> {
        if (items.isEmpty()) return emptyList()

        val orderIndex = order.withIndex().associate { it.value to it.index }
        return items.sortedWith { left, right ->
            val leftIndex = orderIndex[keySelector(left)] ?: Int.MAX_VALUE
            val rightIndex = orderIndex[keySelector(right)] ?: Int.MAX_VALUE
            when {
                leftIndex != rightIndex -> leftIndex.compareTo(rightIndex)
                else -> fallback.compare(left, right)
            }
        }
    }
}
