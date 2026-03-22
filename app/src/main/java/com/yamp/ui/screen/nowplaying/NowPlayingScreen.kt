package com.yamp.ui.screen.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yamp.ui.components.ArtworkType
import com.yamp.player.RepeatMode
import com.yamp.ui.components.ProgressSlider
import com.yamp.ui.components.YampArtwork
import com.yamp.ui.theme.DarkBackground
import com.yamp.ui.theme.Dimensions
import com.yamp.ui.theme.TextSecondary
import com.yamp.ui.theme.TextTertiary

@Composable
fun NowPlayingScreen(
    viewModel: NowPlayingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val track = state.currentTrack

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(Dimensions.paddingXLarge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Album Art
        YampArtwork(
            model = track?.albumArtUri,
            title = track?.title ?: "Now playing",
            type = ArtworkType.TRACK,
            modifier = Modifier
                .size(Dimensions.albumArtLarge),
            size = Dimensions.albumArtLarge,
            mimeType = track?.mimeType.orEmpty(),
            sourcePath = track?.sourcePath.orEmpty(),
            contentDescription = track?.album
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Track Info
        Text(
            text = track?.title ?: "No track playing",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = track?.artist ?: "",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Text(
            text = track?.album ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = TextTertiary,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Progress
        ProgressSlider(
            positionMs = state.positionMs,
            durationMs = state.durationMs,
            onSeek = viewModel::onSeek
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = viewModel::onToggleShuffle) {
                Icon(
                    Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (state.shuffleEnabled) MaterialTheme.colorScheme.primary
                           else TextTertiary
                )
            }

            IconButton(
                onClick = viewModel::onPrevious,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    modifier = Modifier.size(36.dp)
                )
            }

            IconButton(
                onClick = viewModel::onPlayPause,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            IconButton(
                onClick = viewModel::onNext,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "Next",
                    modifier = Modifier.size(36.dp)
                )
            }

            IconButton(onClick = viewModel::onToggleRepeat) {
                Icon(
                    when (state.repeatMode) {
                        RepeatMode.ONE -> Icons.Default.RepeatOne
                        else -> Icons.Default.Repeat
                    },
                    contentDescription = "Repeat",
                    tint = if (state.repeatMode != RepeatMode.OFF)
                        MaterialTheme.colorScheme.primary else TextTertiary
                )
            }
        }
    }
}
