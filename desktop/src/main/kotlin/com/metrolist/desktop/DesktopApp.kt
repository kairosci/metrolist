package com.metrolist.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.metrolist.desktop.core.DesktopPlayer
import com.metrolist.desktop.core.NavigationManager
import com.metrolist.desktop.core.Screen
import com.metrolist.desktop.ui.components.DesktopSection
import com.metrolist.desktop.ui.components.Sidebar
import com.metrolist.desktop.ui.player.PlayerBar
import com.metrolist.desktop.ui.screens.*
import com.metrolist.desktop.ui.theme.MetrolistDesktopTheme

@Composable
fun DesktopApp() {
    MetrolistDesktopTheme {
        val navState by NavigationManager.state.collectAsState()
        val playerState by DesktopPlayer.state.collectAsState()

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Sidebar(
                    selectedSection = navState.currentSection,
                    onSectionSelected = { NavigationManager.navigateToSection(it) },
                )

                VerticalDivider()

                Column(
                    modifier = Modifier.weight(1f).fillMaxSize(),
                ) {
                    when (val screen = navState.currentScreen) {
                        is Screen.Home -> HomeScreen(modifier = Modifier.weight(1f))
                        is Screen.Search -> SearchScreen(modifier = Modifier.weight(1f))
                        is Screen.Library -> LibraryScreen(modifier = Modifier.weight(1f))
                        is Screen.Settings -> SettingsScreen(modifier = Modifier.weight(1f))
                        is Screen.Album -> AlbumScreen(browseId = screen.browseId, modifier = Modifier.weight(1f))
                        is Screen.Artist -> ArtistScreen(browseId = screen.browseId, modifier = Modifier.weight(1f))
                        is Screen.Playlist -> PlaylistScreen(playlistId = screen.playlistId, modifier = Modifier.weight(1f))
                        is Screen.Explore -> ExploreScreen(modifier = Modifier.weight(1f))
                        is Screen.Charts -> ChartsScreen(modifier = Modifier.weight(1f))
                        is Screen.NewReleases -> NewReleaseScreen(modifier = Modifier.weight(1f))
                        is Screen.MoodAndGenres -> MoodAndGenresScreen(modifier = Modifier.weight(1f))
                        is Screen.Podcast -> PodcastScreen(browseId = screen.browseId, modifier = Modifier.weight(1f))
                        is Screen.ListenTogether -> ListenTogetherScreen(modifier = Modifier.weight(1f))
                        is Screen.Equalizer -> EqualizerScreen(modifier = Modifier.weight(1f))
                        is Screen.History -> HistoryScreen(modifier = Modifier.weight(1f))
                        is Screen.NowPlaying -> NowPlayingScreen(modifier = Modifier.weight(1f))
                    }

                    if (playerState.showQueue) {
                        QueueScreen(modifier = Modifier.fillMaxWidth())
                    } else {
                        PlayerBar(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}
