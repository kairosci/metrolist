package com.metrolist.music.ui.screens.video

import androidx.compose.runtime.Immutable

@Immutable
sealed class VideoScreens(val route: String) {
    object Home : VideoScreens("video_home")
    object Search : VideoScreens("video_search")
    object Player : VideoScreens("video_player/{videoId}") {
        fun createRoute(videoId: String) = "video_player/$videoId"
    }
    object FullQueue : VideoScreens("video_queue")
}
