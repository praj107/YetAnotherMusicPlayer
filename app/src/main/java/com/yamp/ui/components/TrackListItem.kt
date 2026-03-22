package com.yamp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.yamp.domain.model.Track
import com.yamp.ui.theme.Dimensions
import com.yamp.ui.theme.TextSecondary
import com.yamp.ui.theme.TextTertiary

@Composable
fun TrackListItem(
    track: Track,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    artworkType: ArtworkType = ArtworkType.TRACK
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Dimensions.paddingLarge, vertical = Dimensions.paddingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        YampArtwork(
            model = track.albumArtUri,
            title = track.title,
            type = artworkType,
            modifier = Modifier
                .size(Dimensions.albumArtSmall),
            size = Dimensions.albumArtSmall,
            mimeType = track.mimeType,
            sourcePath = track.sourcePath,
            contentDescription = track.album
        )

        Spacer(modifier = Modifier.width(Dimensions.paddingStandard))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isPlaying) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${track.artist} - ${track.album}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = track.durationFormatted,
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary
        )
    }
}
