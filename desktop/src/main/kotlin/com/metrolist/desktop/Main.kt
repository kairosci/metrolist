package com.metrolist.desktop

import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.metrolist.desktop.core.DesktopPlayer
import com.metrolist.desktop.core.NavigationManager
import com.metrolist.desktop.core.SystemTrayManager
import com.metrolist.desktop.ui.components.DesktopSection
import java.awt.Desktop
import java.net.URI

fun main() = application {
    val windowState = rememberWindowState(
        size = DpSize(1280.dp, 860.dp),
    )

    var shouldExit = false

    Window(
        onCloseRequest = {
            shouldExit = true
            exitApplication()
        },
        title = "Metrolist Desktop",
        state = windowState,
        onKeyEvent = { event ->
            when {
                event.isCtrlPressed && event.key == Key.Q && event.type == KeyEventType.KeyUp -> {
                    shouldExit = true
                    exitApplication()
                    true
                }
                event.key == Key.Spacebar && event.type == KeyEventType.KeyUp -> {
                    DesktopPlayer.playPause()
                    true
                }
                event.isCtrlPressed && event.key == Key.N && event.type == KeyEventType.KeyUp -> {
                    DesktopPlayer.next()
                    true
                }
                event.isCtrlPressed && event.key == Key.P && event.type == KeyEventType.KeyUp -> {
                    DesktopPlayer.previous()
                    true
                }
                event.isCtrlPressed && event.key == Key.S && event.type == KeyEventType.KeyUp -> {
                    DesktopPlayer.toggleShuffle()
                    true
                }
                event.isCtrlPressed && event.key == Key.R && event.type == KeyEventType.KeyUp -> {
                    DesktopPlayer.toggleRepeat()
                    true
                }
                event.key == Key.F1 && event.type == KeyEventType.KeyUp -> {
                    NavigationManager.navigateToSection(DesktopSection.HOME)
                    true
                }
                event.isCtrlPressed && event.key == Key.T && event.type == KeyEventType.KeyUp -> {
                    NavigationManager.navigateToSection(DesktopSection.SEARCH)
                    true
                }
                event.isCtrlPressed && event.key == Key.L && event.type == KeyEventType.KeyUp -> {
                    NavigationManager.navigateToSection(DesktopSection.LIBRARY)
                    true
                }
                event.isCtrlPressed && event.key == Key.H && event.type == KeyEventType.KeyUp -> {
                    NavigationManager.navigateToHistory()
                    true
                }
                event.isCtrlPressed && event.key == Key.Comma && event.type == KeyEventType.KeyUp -> {
                    NavigationManager.navigateToSection(DesktopSection.SETTINGS)
                    true
                }
                event.isCtrlPressed && event.key == Key.E && event.type == KeyEventType.KeyUp -> {
                    NavigationManager.navigateToEqualizer()
                    true
                }
                event.isCtrlPressed && event.key == Key.M && event.type == KeyEventType.KeyUp -> {
                    NavigationManager.navigateToListenTogether()
                    true
                }
                else -> false
            }
        },
    ) {
        SystemTrayManager.initialize(windowState) {
            shouldExit = true
            exitApplication()
        }

        MenuBar {
            Menu("File") {
                Item("Exit (Ctrl+Q)", onClick = {
                    shouldExit = true
                    exitApplication()
                })
            }
            Menu("Playback") {
                Item("Play/Pause (Space)", onClick = { DesktopPlayer.playPause() })
                Item("Next Track (Ctrl+N)", onClick = { DesktopPlayer.next() })
                Item("Previous Track (Ctrl+P)", onClick = { DesktopPlayer.previous() })
                Separator()
                Item("Shuffle (Ctrl+S)", onClick = { DesktopPlayer.toggleShuffle() })
                Item("Repeat (Ctrl+R)", onClick = { DesktopPlayer.toggleRepeat() })
            }
            Menu("View") {
                Item("Home (F1)", onClick = { NavigationManager.navigateToSection(DesktopSection.HOME) })
                Item("Search (Ctrl+T)", onClick = { NavigationManager.navigateToSection(DesktopSection.SEARCH) })
                Item("Library (Ctrl+L)", onClick = { NavigationManager.navigateToSection(DesktopSection.LIBRARY) })
                Item("History (Ctrl+H)", onClick = { NavigationManager.navigateToHistory() })
                Item("Settings (Ctrl+,)", onClick = { NavigationManager.navigateToSection(DesktopSection.SETTINGS) })
                Separator()
                Item("Explore", onClick = { NavigationManager.navigateToExplore() })
                Item("Charts", onClick = { NavigationManager.navigateToCharts() })
                Item("New Releases", onClick = { NavigationManager.navigateToNewReleases() })
                Item("Moods & Genres", onClick = { NavigationManager.navigateToMoodAndGenres() })
                Separator()
                Item("Toggle Player", onClick = { DesktopPlayer.toggleNowPlaying() })
                Item("Toggle Queue", onClick = { DesktopPlayer.toggleQueue() })
            }
            Menu("Tools") {
                Item("Equalizer (Ctrl+E)", onClick = { NavigationManager.navigateToEqualizer() })
                Item("Listen Together (Ctrl+M)", onClick = { NavigationManager.navigateToListenTogether() })
            }
            Menu("Help") {
                Item("Report Issue", onClick = {
                    try {
                        Desktop.getDesktop().browse(URI("https://github.com/metrolist/metrolist/issues"))
                    } catch (_: Exception) { }
                })
            }
        }

        DesktopApp()
    }
}
