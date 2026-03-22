package com.yamp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.yamp.domain.media.trackFormatBadge
import com.yamp.ui.theme.AccentCyan
import com.yamp.ui.theme.AccentPurple
import com.yamp.ui.theme.DarkCard
import com.yamp.ui.theme.DarkSurfaceVariant
import com.yamp.ui.theme.Dimensions
import com.yamp.ui.theme.TextPrimary

enum class ArtworkType {
    TRACK,
    FOLDER,
    ALBUM,
    ARTIST,
    GENRE
}

@Composable
fun YampArtwork(
    model: String?,
    title: String,
    type: ArtworkType,
    size: Dp,
    modifier: Modifier = Modifier,
    mimeType: String = "",
    sourcePath: String = "",
    contentDescription: String? = null
) {
    val shape = RoundedCornerShape(if (size >= Dimensions.albumArtLarge) 24.dp else 12.dp)
    val badge = if (type == ArtworkType.TRACK) {
        trackFormatBadge(sourcePath = sourcePath, mimeType = mimeType)
    } else {
        null
    }
    val placeholderSpec = remember(title, type, badge) {
        placeholderSpec(title = title, type = type, badge = badge)
    }

    if (model.isNullOrBlank()) {
        ArtworkPlaceholder(
            icon = placeholderSpec.icon,
            badge = placeholderSpec.badge,
            colors = placeholderSpec.colors,
            modifier = modifier
                .clip(shape)
                .background(Brush.linearGradient(placeholderSpec.colors))
                .padding(Dimensions.paddingSmall)
        )
        return
    }

    SubcomposeAsyncImage(
        model = model,
        contentDescription = contentDescription ?: title,
        modifier = modifier.clip(shape),
        contentScale = ContentScale.Crop,
        loading = {
            ArtworkPlaceholder(
                icon = placeholderSpec.icon,
                badge = placeholderSpec.badge,
                colors = placeholderSpec.colors,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(placeholderSpec.colors))
                    .padding(Dimensions.paddingSmall)
            )
        },
        error = {
            ArtworkPlaceholder(
                icon = placeholderSpec.icon,
                badge = placeholderSpec.badge,
                colors = placeholderSpec.colors,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(placeholderSpec.colors))
                    .padding(Dimensions.paddingSmall)
            )
        }
    )
}

private data class PlaceholderSpec(
    val icon: ImageVector,
    val badge: String?,
    val colors: List<Color>
)

@Composable
private fun ArtworkPlaceholder(
    icon: ImageVector,
    badge: String?,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextPrimary.copy(alpha = 0.92f),
            modifier = Modifier
                .align(Alignment.Center)
                .size(Dimensions.iconXLarge)
        )

        if (!badge.isNullOrBlank()) {
            BadgeChip(
                text = badge,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
private fun BoxScope.BadgeChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(DarkCard.copy(alpha = 0.82f))
            .padding(horizontal = Dimensions.paddingStandard, vertical = Dimensions.paddingSmall),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun placeholderSpec(
    title: String,
    type: ArtworkType,
    badge: String?
): PlaceholderSpec {
    val palettes = listOf(
        listOf(AccentPurple.copy(alpha = 0.95f), AccentCyan.copy(alpha = 0.75f)),
        listOf(AccentCyan.copy(alpha = 0.95f), DarkSurfaceVariant.copy(alpha = 0.95f)),
        listOf(DarkCard.copy(alpha = 0.95f), AccentPurple.copy(alpha = 0.85f))
    )
    val colors = palettes[(title.hashCode().absoluteValue + type.ordinal) % palettes.size]
    val icon = when (type) {
        ArtworkType.TRACK -> Icons.Rounded.AudioFile
        ArtworkType.FOLDER -> Icons.Rounded.Folder
        ArtworkType.ALBUM -> Icons.Rounded.Album
        ArtworkType.ARTIST -> Icons.Rounded.Person
        ArtworkType.GENRE -> Icons.Rounded.LibraryMusic
    }

    return PlaceholderSpec(
        icon = icon,
        badge = badge,
        colors = colors
    )
}

private val Int.absoluteValue: Int
    get() = if (this < 0) -this else this
