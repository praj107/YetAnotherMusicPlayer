package com.yamp.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.yamp.domain.model.Track
import com.yamp.player.RepeatMode
import com.yamp.ui.theme.DarkBackground
import com.yamp.ui.theme.DarkSurface
import com.yamp.ui.theme.TextSecondary
import com.yamp.ui.theme.TextTertiary
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private val MiniBarHeight = 64.dp

@Composable
fun BottomPlayerSheet(
    currentTrack: Track?,
    isPlaying: Boolean,
    progress: Float,
    positionMs: Long,
    durationMs: Long,
    repeatMode: RepeatMode,
    shuffleEnabled: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onStop: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = currentTrack != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        if (!isExpanded) {
            MiniPlayerBar(
                track = currentTrack,
                isPlaying = isPlaying,
                progress = progress,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onExpand = { isExpanded = true },
                onDismiss = onStop
            )
        }
    }

    // Full-screen expanded player
    AnimatedVisibility(
        visible = isExpanded && currentTrack != null,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(300)
        ),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(300)
        )
    ) {
        BackHandler { isExpanded = false }

        ExpandedPlayer(
            track = currentTrack,
            isPlaying = isPlaying,
            positionMs = positionMs,
            durationMs = durationMs,
            repeatMode = repeatMode,
            shuffleEnabled = shuffleEnabled,
            onPlayPause = onPlayPause,
            onNext = onNext,
            onPrevious = onPrevious,
            onSeek = onSeek,
            onToggleRepeat = onToggleRepeat,
            onToggleShuffle = onToggleShuffle,
            onCollapse = { isExpanded = false }
        )
    }
}

@Composable
private fun MiniPlayerBar(
    track: Track?,
    isPlaying: Boolean,
    progress: Float,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onExpand: () -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val dismissOffset = remember { Animatable(0f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(dismissOffset.value.roundToInt(), 0) }
            .alpha(1f - (abs(dismissOffset.value) / 800f).coerceIn(0f, 1f))
            .background(DarkSurface, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .pointerInput(Unit) {
                var dragX = 0f
                var dragY = 0f
                detectDragGestures(
                    onDragStart = { dragX = 0f; dragY = 0f },
                    onDragEnd = {
                        val absX = abs(dragX)
                        val absY = abs(dragY)
                        when {
                            // Left swipe → dismiss
                            absX > absY && dragX < -120f -> {
                                scope.launch {
                                    dismissOffset.animateTo(-1200f, tween(200))
                                    onDismiss()
                                    dismissOffset.snapTo(0f)
                                }
                            }
                            // Up swipe → expand
                            absY > absX && dragY < -80f -> onExpand()
                            // Reset position if not dismissed
                            else -> scope.launch { dismissOffset.animateTo(0f, tween(150)) }
                        }
                    },
                    onDrag = { change, offset ->
                        change.consume()
                        dragX += offset.x
                        dragY += offset.y
                        // Only allow leftward drag visually
                        if (abs(dragX) > abs(dragY) && dragX < 0f) {
                            scope.launch { dismissOffset.snapTo(dragX) }
                        }
                    }
                )
            }
            .clickable(onClick = onExpand)
    ) {
        // Progress bar at the very top
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(MiniBarHeight)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art thumbnail
            YampArtwork(
                model = track?.albumArtUri,
                title = track?.title ?: "Current track",
                type = ArtworkType.TRACK,
                modifier = Modifier
                    .size(44.dp),
                size = 44.dp,
                mimeType = track?.mimeType.orEmpty(),
                sourcePath = track?.sourcePath.orEmpty()
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Track info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track?.title ?: "",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track?.artist ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Play/pause
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ExpandedPlayer(
    track: Track?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    repeatMode: RepeatMode,
    shuffleEnabled: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCollapse: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .pointerInput(Unit) {
                var dragY = 0f
                detectDragGestures(
                    onDragStart = { dragY = 0f },
                    onDragEnd = {
                        if (dragY > 120f) onCollapse()
                    },
                    onDrag = { change, offset ->
                        change.consume()
                        dragY += offset.y
                    }
                )
            }
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Drag handle / collapse button
        IconButton(
            onClick = onCollapse,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Collapse",
                tint = TextSecondary,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Album art
        YampArtwork(
            model = track?.albumArtUri,
            title = track?.title ?: "Now playing",
            type = ArtworkType.TRACK,
            modifier = Modifier
                .size(280.dp),
            size = 280.dp,
            mimeType = track?.mimeType.orEmpty(),
            sourcePath = track?.sourcePath.orEmpty(),
            contentDescription = track?.album
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Track info
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

        // Progress slider
        ProgressSlider(
            positionMs = positionMs,
            durationMs = durationMs,
            onSeek = onSeek
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Playback controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleShuffle) {
                Icon(
                    Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (shuffleEnabled) MaterialTheme.colorScheme.primary
                    else TextTertiary
                )
            }

            IconButton(
                onClick = onPrevious,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    modifier = Modifier.size(36.dp)
                )
            }

            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "Next",
                    modifier = Modifier.size(36.dp)
                )
            }

            IconButton(onClick = onToggleRepeat) {
                Icon(
                    when (repeatMode) {
                        RepeatMode.ONE -> Icons.Default.RepeatOne
                        else -> Icons.Default.Repeat
                    },
                    contentDescription = "Repeat",
                    tint = if (repeatMode != RepeatMode.OFF)
                        MaterialTheme.colorScheme.primary else TextTertiary
                )
            }
        }
    }
}
