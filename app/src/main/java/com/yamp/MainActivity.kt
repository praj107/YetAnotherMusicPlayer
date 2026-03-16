package com.yamp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yamp.player.PlaybackManager
import com.yamp.ui.components.TopPlaybackBar
import com.yamp.ui.navigation.BottomNavBar
import com.yamp.ui.navigation.Screen
import com.yamp.ui.navigation.YampNavHost
import com.yamp.ui.theme.DarkBackground
import com.yamp.ui.theme.YampTheme
import com.yamp.player.currentTrack
import com.yamp.player.isPlaying
import com.yamp.player.PlaybackState
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check for updates on cold start only
        if (savedInstanceState == null) {
            lifecycleScope.launch {
                try {
                    updateManager.checkForUpdate()
                } catch (_: Exception) {
                    // Silent fail - update check is non-critical
                }
            }
        }

        setContent {
            YampTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val pbState by playbackManager.playbackState.collectAsState()

                val currentTrack = pbState.currentTrack
                val isPlaying = pbState.isPlaying
                val progress = when (val s = pbState) {
                    is PlaybackState.Playing -> if (s.durationMs > 0) s.positionMs.toFloat() / s.durationMs else 0f
                    is PlaybackState.Paused -> if (s.durationMs > 0) s.positionMs.toFloat() / s.durationMs else 0f
                    else -> 0f
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground),
                    containerColor = DarkBackground,
                    topBar = {
                        if (currentRoute != Screen.NowPlaying.route) {
                            TopPlaybackBar(
                                currentTrack = currentTrack,
                                isPlaying = isPlaying,
                                progress = progress,
                                onPlayPause = {
                                    if (isPlaying) playbackManager.pause()
                                    else playbackManager.play()
                                },
                                onNext = { playbackManager.skipNext() },
                                onPrevious = { playbackManager.skipPrevious() },
                                onBarClick = {
                                    navController.navigate(Screen.NowPlaying.route)
                                }
                            )
                        }
                    },
                    bottomBar = {
                        if (currentRoute != Screen.NowPlaying.route) {
                            BottomNavBar(
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    navController.navigate(route) {
                                        popUpTo(Screen.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        YampNavHost(
                            navController = navController,
                            onTrackClick = { track, queue ->
                                val index = queue.indexOf(track).coerceAtLeast(0)
                                playbackManager.playTrack(track, queue, index)
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            playbackManager.release()
        }
    }
}
