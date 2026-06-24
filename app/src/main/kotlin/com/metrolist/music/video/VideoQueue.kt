package com.metrolist.music.video

data class VideoQueue(
    val videos: List<VideoMetadata>,
    val currentIndex: Int = 0,
) {
    val currentVideo: VideoMetadata?
        get() = videos.getOrNull(currentIndex)

    val hasNext: Boolean
        get() = currentIndex < videos.size - 1

    val hasPrevious: Boolean
        get() = currentIndex > 0

    fun next(): VideoQueue =
        if (hasNext) copy(currentIndex = currentIndex + 1) else this

    fun previous(): VideoQueue =
        if (hasPrevious) copy(currentIndex = currentIndex - 1) else this

    fun seekTo(index: Int): VideoQueue =
        if (index in videos.indices) copy(currentIndex = index) else this
}
