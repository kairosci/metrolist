package com.metrolist.shared.carplay

import com.metrolist.shared.db.MusicDatabase
import com.metrolist.shared.db.entities.Playlist
import com.metrolist.shared.db.entities.toSongItem
import com.metrolist.shared.db.hasCompleteStoredSongs
import com.metrolist.shared.db.loadAndCachePlaylistPage
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.liked_songs
import com.metrolist.shared.generated.resources.now_playing
import com.metrolist.shared.generated.resources.playlists
import com.metrolist.shared.generated.resources.songs
import com.metrolist.shared.model.RepeatMode
import com.metrolist.shared.model.SongItem
import com.metrolist.shared.model.playlistOccurrenceIdentity
import com.metrolist.shared.playback.IosPlaybackController
import com.metrolist.shared.playback.MusicPlayer
import com.metrolist.shared.state.GlobalYouTubeRepository
import com.metrolist.shared.storage.IosAudioDownloadManager
import com.metrolist.shared.ui.playback.PlaybackController
import com.metrolist.shared.ui.playback.PlaybackUiState
import com.metrolist.shared.youtube.logging.YtLogger
import com.metrolist.shared.youtube.playback.StreamResolver
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.koin.mp.KoinPlatform

data class CarPlayStrings(
    val likedSongs: String,
    val playlists: String,
    val songs: String,
    val nowPlaying: String,
)

data class CarPlayBrowseItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val isPlayable: Boolean = false,
    val isContainer: Boolean = false,
    val songId: String? = null,
    val occurrenceId: String? = null,
)

data class CarPlayNowPlayingState(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val isPlaying: Boolean = false,
    val canSkipNext: Boolean = false,
    val canSkipPrevious: Boolean = false,
    val isShuffled: Boolean = false,
    val repeatMode: String = RepeatMode.OFF.name,
)

object IosCarPlayBridge {
    private val lock = SynchronizedObject()
    private var controller: PlaybackController? = null
    private var database: MusicDatabase? = null
    private var scope: CoroutineScope? = null
    private var strings: CarPlayStrings? = null
    private val stateListeners = linkedSetOf<(CarPlayNowPlayingState) -> Unit>()
    private val pendingStringCompletions = mutableListOf<(CarPlayStrings) -> Unit>()
    private var stateJob: Job? = null
    private var songSelectionGeneration = 0L
    private val playlistFetchMutex = Mutex()

    fun initializeForCarPlay() {
        val needsInitialization = synchronized(lock) { scope == null }
        if (!needsInitialization) return
        val koin = KoinPlatform.getKoin()
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val activeDatabase = koin.get<MusicDatabase>()
        val activeController =
            IosPlaybackController(
                player = koin.get<MusicPlayer>(),
                resolver = koin.get<StreamResolver>(),
                database = activeDatabase,
                scope = appScope,
                audioStorage = IosAudioDownloadManager(activeDatabase, appScope),
                musicVideoResolver = koin.get(),
            )
        register(activeController, activeDatabase, appScope)
    }

    fun register(
        controller: PlaybackController,
        database: MusicDatabase,
        parentScope: CoroutineScope,
    ) {
        val bridgeScope = CoroutineScope(SupervisorJob(parentScope.coroutineContext[Job]) + Dispatchers.Main.immediate)
        synchronized(lock) {
            unregisterLocked()
            this.controller = controller
            this.database = database
            this.scope = bridgeScope
            stateJob =
                bridgeScope.launch {
                    combine(controller.state, controller.playbackCommands) { state, _ ->
                        state.toCarPlayState(controller)
                    }.collect { snapshot ->
                        val listeners = synchronized(lock) { stateListeners.toList() }
                        listeners.forEach { it(snapshot) }
                    }
                }
        }
        bridgeScope.launch {
            publishStrings(loadCarPlayStrings())
        }
    }

    fun unregister() {
        synchronized(lock) { unregisterLocked() }
    }

    private fun unregisterLocked() {
        stateJob?.cancel()
        stateJob = null
        scope?.cancel()
        scope = null
        controller = null
        database = null
        strings = null
    }

    fun currentStrings(): CarPlayStrings? = synchronized(lock) { strings }

    private suspend fun loadCarPlayStrings(): CarPlayStrings =
        CarPlayStrings(
            likedSongs = getString(Res.string.liked_songs),
            playlists = getString(Res.string.playlists),
            songs = getString(Res.string.songs),
            nowPlaying = getString(Res.string.now_playing),
        )

    private fun publishStrings(loaded: CarPlayStrings) {
        val pending =
            synchronized(lock) {
                strings = loaded
                pendingStringCompletions.toList().also { pendingStringCompletions.clear() }
            }
        pending.forEach { it(loaded) }
    }

    fun loadStrings(completion: (CarPlayStrings) -> Unit) {
        var cached: CarPlayStrings? = null
        var launchScope: CoroutineScope? = null
        synchronized(lock) {
            val current = strings
            if (current != null) {
                cached = current
            } else if (scope == null) {
                pendingStringCompletions += completion
            } else {
                launchScope = scope
            }
        }
        cached?.let {
            completion(it)
            return
        }
        val activeScope = launchScope ?: return
        activeScope.launch {
            val loaded = loadCarPlayStrings()
            publishStrings(loaded)
            completion(loaded)
        }
    }

    fun addStateListener(listener: (CarPlayNowPlayingState) -> Unit): () -> Unit {
        val current =
            synchronized(lock) {
                stateListeners += listener
                controller?.let { it.state.value.toCarPlayState(it) }
            }
        if (current != null) listener(current)
        return {
            synchronized(lock) { stateListeners -= listener }
        }
    }

    fun currentState(): CarPlayNowPlayingState =
        synchronized(lock) {
            controller?.let { it.state.value.toCarPlayState(it) }
        } ?: CarPlayNowPlayingState()

    fun loadRootItems(completion: (List<CarPlayBrowseItem>) -> Unit) {
        loadStrings { s ->
            completion(
                listOf(
                    CarPlayBrowseItem(id = LIKED_SONGS_ID, title = s.likedSongs, isContainer = true),
                    CarPlayBrowseItem(id = PLAYLISTS_ID, title = s.playlists, isContainer = true),
                ),
            )
        }
    }

    fun loadChildren(
        parentId: String,
        completion: (List<CarPlayBrowseItem>) -> Unit,
    ) {
        val (activeScope, db) =
            synchronized(lock) {
                val currentScope = scope
                val currentDatabase = database
                if (currentScope == null || currentDatabase == null) {
                    null to null
                } else {
                    currentScope to currentDatabase
                }
            }
        if (activeScope == null || db == null) {
            completion(emptyList())
            return
        }
        activeScope.launch {
            val items =
                when (parentId) {
                    LIKED_SONGS_ID -> {
                        loadLikedSongs(db)
                    }

                    PLAYLISTS_ID -> {
                        loadPlaylists(db)
                    }

                    else -> {
                        ensureRemotePlaylistSongs(db, parentId)
                        loadPlaylistSongs(db, parentId)
                    }
                }
            completion(items)
        }
    }

    fun playSongFromContainer(
        containerId: String,
        songId: String,
        occurrenceId: String?,
    ) {
        var activeScope: CoroutineScope? = null
        var db: MusicDatabase? = null
        var playback: PlaybackController? = null
        var selectionGeneration = 0L
        synchronized(lock) {
            activeScope = scope
            db = database
            playback = controller
            if (activeScope == null || db == null || playback == null) return
            selectionGeneration = ++songSelectionGeneration
        }
        val currentScope = activeScope ?: return
        val currentDatabase = db ?: return
        val currentPlayback = playback ?: return
        currentScope.launch {
            val songs =
                if (containerId == LIKED_SONGS_ID) {
                    loadLikedSongItems(currentDatabase)
                } else {
                    ensureRemotePlaylistSongs(currentDatabase, containerId)
                    loadPlaylistSongItems(currentDatabase, containerId)
                }
            val index =
                songs.indexOfFirst { song ->
                    song.id == songId && (occurrenceId == null || song.playlistOccurrenceIdentity() == occurrenceId)
                }
            val isCurrentSelection = synchronized(lock) { selectionGeneration == songSelectionGeneration }
            if (index >= 0 && isCurrentSelection) currentPlayback.playQueue(songs, index)
        }
    }

    fun togglePlayPause() {
        synchronized(lock) { controller }?.togglePlayPause()
    }

    fun skipToNext() {
        synchronized(lock) { controller }?.skipToNext()
    }

    fun skipToPrevious() {
        synchronized(lock) { controller }?.skipToPrevious()
    }

    private suspend fun loadLikedSongs(db: MusicDatabase): List<CarPlayBrowseItem> =
        withContext(Dispatchers.Default) {
            db.dao().likedSongsByCreateDateAsc().first().map { song ->
                val item = song.toSongItem()
                CarPlayBrowseItem(
                    id = "$LIKED_SONGS_ID|${item.id}",
                    title = item.title,
                    subtitle = item.artists.joinToString { it.name },
                    isPlayable = true,
                    songId = item.id,
                    occurrenceId = item.playlistOccurrenceIdentity(),
                )
            }
        }

    private suspend fun loadLikedSongItems(db: MusicDatabase): List<SongItem> =
        withContext(Dispatchers.Default) {
            db
                .dao()
                .likedSongsByCreateDateAsc()
                .first()
                .map { it.toSongItem() }
        }

    private suspend fun loadPlaylists(db: MusicDatabase): List<CarPlayBrowseItem> =
        withContext(Dispatchers.Default) {
            db
                .dao()
                .playlistsByCreateDateAsc()
                .first()
                .map { it.toBrowseItem() }
        }

    private suspend fun ensureRemotePlaylistSongs(
        db: MusicDatabase,
        playlistId: String,
    ) {
        playlistFetchMutex.withLock {
            val entity = db.dao().findPlaylistEntity(playlistId) ?: return@withLock
            if (entity.isLocal || entity.browseId == null) return@withLock
            val storedSongs =
                db
                    .dao()
                    .playlistSongs(playlistId)
                    .first()
                    .map { it.toSongItem() }
            if (entity.hasCompleteStoredSongs(storedSongs)) return@withLock
            try {
                db.loadAndCachePlaylistPage(GlobalYouTubeRepository.instance, playlistId)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                YtLogger.e("CarPlay", "Playlist fetch failed type=${error::class.simpleName}")
            }
        }
    }

    private suspend fun loadPlaylistSongs(
        db: MusicDatabase,
        playlistId: String,
    ): List<CarPlayBrowseItem> =
        withContext(Dispatchers.Default) {
            db.dao().playlistSongs(playlistId).first().map { ps ->
                val item = ps.toSongItem()
                CarPlayBrowseItem(
                    id = "$playlistId|${item.id}",
                    title = item.title,
                    subtitle = item.artists.joinToString { it.name },
                    isPlayable = true,
                    songId = item.id,
                    occurrenceId = item.playlistOccurrenceIdentity(),
                )
            }
        }

    private suspend fun loadPlaylistSongItems(
        db: MusicDatabase,
        playlistId: String,
    ): List<SongItem> =
        withContext(Dispatchers.Default) {
            db
                .dao()
                .playlistSongs(playlistId)
                .first()
                .map { it.toSongItem() }
        }

    private fun Playlist.toBrowseItem(): CarPlayBrowseItem =
        CarPlayBrowseItem(
            id = playlist.id,
            title = playlist.name,
            subtitle = songCount.takeIf { it > 0 }?.toString(),
            isContainer = true,
        )

    private fun PlaybackUiState.toCarPlayState(controller: PlaybackController): CarPlayNowPlayingState =
        CarPlayNowPlayingState(
            title = currentSong?.title,
            artist = currentSong?.artists?.joinToString { it.name },
            album = currentSong?.album?.name,
            isPlaying = isPlaying,
            canSkipNext = controller.canSkipNext,
            canSkipPrevious = controller.canSkipPrevious,
            isShuffled = isShuffled,
            repeatMode = repeatMode.name,
        )

    private const val LIKED_SONGS_ID = "liked_songs"
    private const val PLAYLISTS_ID = "playlists"
}
