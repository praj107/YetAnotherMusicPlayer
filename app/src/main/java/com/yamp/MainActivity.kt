package com.yamp

import android.media.AudioManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yamp.crash.CrashReporter
import com.yamp.player.PlaybackManager
import com.yamp.player.PlaybackState
import com.yamp.player.currentTrack
import com.yamp.player.isPlaying
import com.yamp.ui.components.BottomPlayerSheet
import com.yamp.ui.components.CrashReportDialog
import com.yamp.ui.navigation.BottomNavBar
import com.yamp.ui.navigation.YampNavHost
import com.yamp.ui.theme.DarkBackground
import com.yamp.ui.theme.YampTheme
import com.yamp.updater.UpdateManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var playbackManager: PlaybackManager

    @Inject
    lateinit var updateManager: UpdateManager

    @Inject
    lateinit var crashReporter: CrashReporter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (savedInstanceState == null) {
            lifecycleScope.launch {
                try {
                    updateManager.checkForUpdate()
                } catch (_: Exception) { }
            }
        }

        setContent {
            YampTheme {
                val context = LocalContext.current
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val pbState by playbackManager.playbackState.collectAsState()
                val pendingCrashReport by crashReporter.pendingReport.collectAsState()

                val currentTrack = pbState.currentTrack
                val isPlaying = pbState.isPlaying
                val positionMs = when (val s = pbState) {
                    is PlaybackState.Playing -> s.positionMs
                    is PlaybackState.Paused -> s.positionMs
                    else -> 0L
                }
                val durationMs = when (val s = pbState) {
                    is PlaybackState.Playing -> s.durationMs
                    is PlaybackState.Paused -> s.durationMs
                    else -> 0L
                }
                val progress = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground)
                        .windowInsetsPadding(WindowInsets.systemBars)
                ) {
                    pendingCrashReport?.let { report ->
                        CrashReportDialog(
                            report = report,
                            onOpenIssue = {
                                val copied = crashReporter.copyReportToClipboard(report)
                                if (copied) {
                                    Toast.makeText(
                                        context,
                                        "Crash report copied to clipboard for quick paste.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                context.startActivity(crashReporter.buildIssueIntent(report))
                            },
                            onShareReport = {
                                crashReporter.buildShareIntent(report)?.let { shareIntent ->
                                    context.startActivity(
                                        android.content.Intent.createChooser(
                                            shareIntent,
                                            "Share crash report"
                                        )
                                    )
                                }
                            },
                            onDismiss = crashReporter::dismissPendingReport
                        )
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Main content fills remaining space
                        Box(modifier = Modifier.weight(1f)) {
                            YampNavHost(
                                navController = navController,
                                onTrackClick = { track, queue ->
                                    val index = queue.indexOf(track).coerceAtLeast(0)
                                    playbackManager.playTrack(track, queue, index)
                                }
                            )
                        }

                        // Mini player bar above bottom nav
                        BottomPlayerSheet(
                            currentTrack = currentTrack,
                            isPlaying = isPlaying,
                            progress = progress,
                            positionMs = positionMs,
                            durationMs = durationMs,
                            repeatMode = playbackManager.repeatMode,
                            shuffleEnabled = playbackManager.queueManager.shuffleEnabled,
                            onPlayPause = {
                                if (isPlaying) playbackManager.pause()
                                else playbackManager.play()
                            },
                            onNext = { playbackManager.skipNext() },
                            onPrevious = { playbackManager.skipPrevious() },
                            onStop = { playbackManager.stop() },
                            onSeek = { playbackManager.seekTo(it) },
                            onToggleRepeat = { playbackManager.toggleRepeat() },
                            onToggleShuffle = { playbackManager.toggleShuffle() }
                        )

                        // Bottom navigation
                        BottomNavBar(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        volumeControlStream = AudioManager.STREAM_MUSIC
    }
}
