package com.yamp.ui.screen.search

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
import com.yamp.ui.components.YampSearchBar
import com.yamp.ui.theme.Dimensions
import com.yamp.ui.theme.TextSecondary

@Composable
fun SearchScreen(
    onTrackClick: (Track, List<Track>) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        YampSearchBar(
            query = state.query,
            onQueryChange = viewModel::onQueryChange
        )

        if (state.isSearching) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(Dimensions.paddingLarge),
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (state.query.isNotEmpty() && state.results.isEmpty() && !state.isSearching) {
            Text(
                text = "No results found",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(Dimensions.paddingLarge)
            )
        }

        LazyColumn {
            items(state.results) { track ->
                TrackListItem(
                    track = track,
                    onClick = { onTrackClick(track, state.results) }
                )
            }
        }
    }
}
