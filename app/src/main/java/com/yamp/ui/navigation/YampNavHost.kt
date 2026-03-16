package com.yamp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yamp.domain.model.Track
import com.yamp.ui.screen.home.HomeScreen
import com.yamp.ui.screen.library.LibraryScreen
import com.yamp.ui.screen.nowplaying.NowPlayingScreen
import com.yamp.ui.screen.playlist.PlaylistScreen
import com.yamp.ui.screen.search.SearchScreen
import com.yamp.ui.screen.settings.SettingsScreen

@Composable
fun YampNavHost(
    navController: NavHostController,
    onTrackClick: (Track, List<Track>) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onTrackClick = onTrackClick,
                onPlaylistClick = { id ->
                    navController.navigate(Screen.Playlist.createRoute(id))
                }
            )
        }

        composable(Screen.Library.route) {
            LibraryScreen(onTrackClick = onTrackClick)
        }

        composable(Screen.Search.route) {
            SearchScreen(onTrackClick = onTrackClick)
        }

        composable(Screen.NowPlaying.route) {
            NowPlayingScreen()
        }

        composable(Screen.Settings.route) {
            SettingsScreen()
        }

        composable(
            route = Screen.Playlist.route,
            arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
        ) {
            PlaylistScreen(onTrackClick = onTrackClick)
        }
    }
}
