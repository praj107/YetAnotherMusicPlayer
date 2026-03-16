package com.yamp.ui.screen.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.yamp.domain.model.Track
import com.yamp.ui.components.SortChipRow
import com.yamp.ui.components.TrackListItem
import com.yamp.ui.theme.DarkSurface
import com.yamp.ui.theme.Dimensions
import com.yamp.ui.theme.TextSecondary
import com.yamp.ui.theme.TextTertiary

@Composable
fun LibraryScreen(
    onTrackClick: (Track, List<Track>) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
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
                            color = if (state.selectedTab == index)
                                MaterialTheme.colorScheme.primary else TextTertiary
                        )
                    }
                )
            }
        }

        when (state.selectedTab) {
            0 -> {
                SortChipRow(
                    selectedField = state.currentSort.field,
                    onFieldSelected = viewModel::onSortFieldSelected
                )
                LazyColumn {
                    items(state.tracks) { track ->
                        TrackListItem(
                            track = track,
                            onClick = { onTrackClick(track, state.tracks) }
                        )
                    }
                }
            }
            1 -> {
                LazyColumn {
                    items(state.albums) { album ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTrackClick(album.tracks.first(), album.tracks) }
                                .padding(
                                    horizontal = Dimensions.paddingLarge,
                                    vertical = Dimensions.paddingStandard
                                )
                        ) {
                            Column {
                                Text(album.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${album.artist} - ${album.tracks.size} tracks",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
            2 -> {
                LazyColumn {
                    items(state.artists) { artist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = Dimensions.paddingLarge,
                                    vertical = Dimensions.paddingStandard
                                )
                        ) {
                            Column {
                                Text(artist.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${artist.trackCount} tracks, ${artist.albumCount} albums",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
            3 -> {
                LazyColumn {
                    items(state.genres) { genre ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = Dimensions.paddingLarge,
                                    vertical = Dimensions.paddingStandard
                                )
                        ) {
                            Column {
                                Text(genre.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${genre.trackCount} tracks",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
