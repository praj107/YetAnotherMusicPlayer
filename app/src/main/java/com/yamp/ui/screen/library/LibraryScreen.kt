package com.yamp.ui.screen.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yamp.domain.model.Album
import com.yamp.domain.model.Artist
import com.yamp.domain.model.Genre
import com.yamp.domain.model.Track
import com.yamp.ui.components.ArtworkType
import com.yamp.ui.components.SortChipRow
import com.yamp.ui.components.TrackListItem
import com.yamp.ui.components.YampArtwork
import com.yamp.ui.theme.DarkCard
import com.yamp.ui.theme.DarkSurface
import com.yamp.ui.theme.Dimensions
import com.yamp.ui.theme.TextSecondary
import com.yamp.ui.theme.TextTertiary

@Composable
fun LibraryScreen(
    onTrackClick: (Track, List<Track>) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val tabs = listOf("Songs", "Albums", "Artists", "Genres")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = state.selectedTab,
            containerColor = DarkSurface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            indicator = { tabPositions ->
                SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[state.selectedTab]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = state.selectedTab == index,
                    onClick = { viewModel.onTabSelected(index) },
                    text = {
                        Text(
                            title,
                            color = if (state.selectedTab == index) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                TextTertiary
                            }
                        )
                    }
                )
            }
        }

        state.selectedCollection?.let { detail ->
            CollectionDetailView(
                detail = detail,
                onBack = viewModel::onCollectionBack,
                onTrackClick = onTrackClick
            )
            return@Column
        }

        when (state.selectedTab) {
            0 -> SongsTab(
                tracks = state.tracks,
                currentSort = state.currentSort.field,
                onSortFieldSelected = viewModel::onSortFieldSelected,
                onTrackClick = onTrackClick
            )

            1 -> AlbumTab(
                albums = state.albums,
                onAlbumClick = viewModel::onAlbumSelected,
                onMove = viewModel::onAlbumMove
            )

            2 -> ArtistTab(
                artists = state.artists,
                onArtistClick = viewModel::onArtistSelected,
                onMove = viewModel::onArtistMove
            )

            3 -> GenreTab(
                genres = state.genres,
                onGenreClick = viewModel::onGenreSelected,
                onMove = viewModel::onGenreMove
            )
        }
    }
}

@Composable
private fun SongsTab(
    tracks: List<Track>,
    currentSort: com.yamp.domain.model.SortField,
    onSortFieldSelected: (com.yamp.domain.model.SortField) -> Unit,
    onTrackClick: (Track, List<Track>) -> Unit
) {
    SortChipRow(
        selectedField = currentSort,
        onFieldSelected = onSortFieldSelected
    )
    LazyColumn {
        items(tracks) { track ->
            TrackListItem(
                track = track,
                onClick = { onTrackClick(track, tracks) }
            )
        }
    }
}

@Composable
private fun AlbumTab(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    onMove: (Int, Int) -> Unit
) {
    ReorderableCollectionList(
        items = albums,
        key = { it.key },
        onMove = onMove,
        onClick = onAlbumClick
    ) { album, isDragging ->
        CollectionCard(
            title = album.name,
            subtitle = "${album.artist} • ${album.tracks.size} tracks",
            artworkModel = album.albumArtUri,
            artworkTitle = album.name,
            artworkType = ArtworkType.ALBUM,
            isDragging = isDragging
        )
    }
}

@Composable
private fun ArtistTab(
    artists: List<Artist>,
    onArtistClick: (Artist) -> Unit,
    onMove: (Int, Int) -> Unit
) {
    ReorderableCollectionList(
        items = artists,
        key = { it.key },
        onMove = onMove,
        onClick = onArtistClick
    ) { artist, isDragging ->
        CollectionCard(
            title = artist.name,
            subtitle = "${artist.trackCount} tracks • ${artist.albumCount} albums",
            artworkModel = artist.artworkUri,
            artworkTitle = artist.name,
            artworkType = ArtworkType.ARTIST,
            isDragging = isDragging
        )
    }
}

@Composable
private fun GenreTab(
    genres: List<Genre>,
    onGenreClick: (Genre) -> Unit,
    onMove: (Int, Int) -> Unit
) {
    ReorderableCollectionList(
        items = genres,
        key = { it.key },
        onMove = onMove,
        onClick = onGenreClick
    ) { genre, isDragging ->
        CollectionCard(
            title = genre.name,
            subtitle = "${genre.trackCount} tracks",
            artworkModel = genre.artworkUri,
            artworkTitle = genre.name,
            artworkType = ArtworkType.GENRE,
            isDragging = isDragging
        )
    }
}

@Composable
private fun CollectionDetailView(
    detail: LibraryCollectionDetail,
    onBack: () -> Unit,
    onTrackClick: (Track, List<Track>) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onBack)
                    .padding(Dimensions.paddingLarge),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(Dimensions.paddingStandard))
                Text(
                    text = "Back to ${detail.type.name.lowercase()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.paddingLarge),
                verticalAlignment = Alignment.CenterVertically
            ) {
                YampArtwork(
                    model = detail.artworkUri,
                    title = detail.title,
                    type = when (detail.type) {
                        com.yamp.domain.model.LibraryCollectionType.ALBUMS -> ArtworkType.ALBUM
                        com.yamp.domain.model.LibraryCollectionType.ARTISTS -> ArtworkType.ARTIST
                        com.yamp.domain.model.LibraryCollectionType.GENRES -> ArtworkType.GENRE
                    },
                    size = 84.dp,
                    modifier = Modifier.size(84.dp)
                )
                Spacer(modifier = Modifier.width(Dimensions.paddingLarge))
                Column {
                    Text(detail.title, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        detail.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.paddingLarge))
        }

        items(detail.tracks) { track ->
            TrackListItem(
                track = track,
                onClick = { onTrackClick(track, detail.tracks) }
            )
        }
    }
}

@Composable
private fun CollectionCard(
    title: String,
    subtitle: String,
    artworkModel: String?,
    artworkTitle: String,
    artworkType: ArtworkType,
    isDragging: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.paddingLarge, vertical = Dimensions.paddingMedium),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                DarkCard
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragging) 10.dp else Dimensions.cardElevation
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.paddingLarge),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingLarge)
        ) {
            YampArtwork(
                model = artworkModel,
                title = artworkTitle,
                type = artworkType,
                size = 64.dp,
                modifier = Modifier.size(64.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(Dimensions.paddingSmall))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(Dimensions.paddingSmall))
                Text(
                    text = "Tap to browse • Hold to reorder",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }
        }
    }
}

@Composable
private fun <T> ReorderableCollectionList(
    items: List<T>,
    key: (T) -> String,
    onMove: (Int, Int) -> Unit,
    onClick: (T) -> Unit,
    card: @Composable (T, Boolean) -> Unit
) {
    var draggingIndex by remember(items) { mutableIntStateOf(-1) }
    var dragOffset by remember(items) { mutableFloatStateOf(0f) }
    var itemHeightPx by remember(items) { mutableFloatStateOf(1f) }

    LazyColumn {
        itemsIndexed(
            items = items,
            key = { _, item -> key(item) }
        ) { index, item ->
            val isDragging = index == draggingIndex

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { size ->
                        if (size.height > 0) {
                            itemHeightPx = size.height.toFloat()
                        }
                    }
                    .graphicsLayer {
                        translationY = if (isDragging) dragOffset else 0f
                    }
                    .zIndex(if (isDragging) 1f else 0f)
                    .pointerInput(items, index) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingIndex = index
                                dragOffset = 0f
                            },
                            onDragEnd = {
                                draggingIndex = -1
                                dragOffset = 0f
                            },
                            onDragCancel = {
                                draggingIndex = -1
                                dragOffset = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (draggingIndex != index) return@detectDragGesturesAfterLongPress

                                dragOffset += dragAmount.y
                                val threshold = itemHeightPx * 0.6f
                                var currentIndex = draggingIndex

                                while (dragOffset > threshold && currentIndex < items.lastIndex) {
                                    onMove(currentIndex, currentIndex + 1)
                                    currentIndex += 1
                                    draggingIndex = currentIndex
                                    dragOffset -= itemHeightPx
                                }

                                while (dragOffset < -threshold && currentIndex > 0) {
                                    onMove(currentIndex, currentIndex - 1)
                                    currentIndex -= 1
                                    draggingIndex = currentIndex
                                    dragOffset += itemHeightPx
                                }
                            }
                        )
                    }
                    .clickable { onClick(item) }
            ) {
                card(item, isDragging)
            }
        }
    }
}
