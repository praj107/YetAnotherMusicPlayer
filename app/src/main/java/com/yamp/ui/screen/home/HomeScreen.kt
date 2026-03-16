package com.yamp.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.yamp.domain.model.Track
import com.yamp.ui.components.RecommendedNextCard
import com.yamp.ui.components.RequestAudioPermission
import com.yamp.ui.components.TrackListItem
import com.yamp.ui.theme.Dimensions
import com.yamp.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    onTrackClick: (Track, List<Track>) -> Unit,
    onPlaylistClick: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    if (!state.permissionGranted) {
        RequestAudioPermission(
            onPermissionGranted = viewModel::onPermissionGranted,
            onPermissionDenied = viewModel::onPermissionDenied
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = Dimensions.paddingLarge)
    ) {
        if (state.isScanning) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Dimensions.paddingXLarge),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(Dimensions.paddingMedium))
                    Text("Scanning for music...", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        if (state.scanProgress.isNotEmpty()) {
            item {
                Text(
                    text = state.scanProgress,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = Dimensions.paddingLarge)
                )
            }
        }

        // Recommended Next Section
        if (state.recommendedNext.isNotEmpty()) {
            item {
                Text(
                    text = "Recommended Next",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(
                        horizontal = Dimensions.paddingLarge,
                        vertical = Dimensions.paddingMedium
                    )
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Dimensions.paddingLarge),
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingStandard)
                ) {
                    items(state.recommendedNext) { track ->
                        RecommendedNextCard(
                            track = track,
                            onClick = { onTrackClick(track, state.recommendedNext) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Dimensions.paddingLarge))
            }
        }

        // Folder Playlists
        if (state.folderPlaylists.isNotEmpty()) {
            item {
                Text(
                    text = "Folders",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(
                        horizontal = Dimensions.paddingLarge,
                        vertical = Dimensions.paddingMedium
                    )
                )
            }
            items(state.folderPlaylists) { playlist ->
                TrackListItem(
                    track = Track(
                        id = playlist.id,
                        contentUri = "",
                        title = playlist.name,
                        artist = "${playlist.trackCount} tracks",
                        album = "",
                        albumArtUri = playlist.coverArtUri,
                        genre = null,
                        duration = 0,
                        trackNumber = null,
                        year = null,
                        mimeType = "",
                        folderPath = "",
                        metadataComplete = true
                    ),
                    onClick = { onPlaylistClick(playlist.id) }
                )
            }
        }

        // Recent Tracks
        if (state.recentTracks.isNotEmpty()) {
            item {
                Text(
                    text = "All Tracks",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(
                        horizontal = Dimensions.paddingLarge,
                        vertical = Dimensions.paddingMedium
                    )
                )
            }
            items(state.recentTracks) { track ->
                TrackListItem(
                    track = track,
                    onClick = { onTrackClick(track, state.recentTracks) }
                )
            }
        }

        // Error
        state.error?.let { error ->
            item {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(Dimensions.paddingLarge)
                )
            }
        }
    }
}
