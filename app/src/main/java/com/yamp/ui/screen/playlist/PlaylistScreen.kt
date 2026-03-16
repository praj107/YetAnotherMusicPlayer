package com.yamp.ui.screen.playlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.yamp.ui.components.TrackListItem
import com.yamp.ui.theme.Dimensions
import com.yamp.ui.theme.TextSecondary

@Composable
fun PlaylistScreen(
    onTrackClick: (Track, List<Track>) -> Unit,
    viewModel: PlaylistViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(Dimensions.paddingLarge),
                color = MaterialTheme.colorScheme.primary
            )
        }

        state.playlist?.let { playlist ->
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(Dimensions.paddingLarge)
            )
            Text(
                text = "${playlist.trackCount} tracks",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = Dimensions.paddingLarge)
            )

            LazyColumn {
                items(playlist.tracks) { track ->
                    TrackListItem(
                        track = track,
                        onClick = { onTrackClick(track, playlist.tracks) }
                    )
                }
            }
        }
    }
}
