package com.moonplayer.app.viewmodel

import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.moonplayer.app.data.db.AppDatabase
import com.moonplayer.app.data.model.*
import com.moonplayer.app.data.preferences.*
import com.moonplayer.app.data.repository.MusicRepository
import com.moonplayer.app.player.PlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = SettingsRepository(application)
    private val db = AppDatabase.getInstance(application)
    private val repo = MusicRepository(application, db.songDao(), db.playlistDao(), settings)
    val player = PlayerManager(application)

    val songs = repo.getAllSongs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val favorites = repo.getFavorites().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val artists = repo.getArtists().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val albums = repo.getAlbums().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val folders = repo.getFolders().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val playlists = repo.getPlaylists().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appSettings = settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())
    val themeMode = appSettings.map { it.theme }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)
    val gapless = appSettings.map { it.gapless }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val crossfade = appSettings.map { it.crossfade }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val autoPlay = appSettings.map { it.autoPlay }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val includePaths = settings.includePaths.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val excludePaths = settings.excludePaths.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    val searchResults = _searchQuery.flatMapLatest { q ->
        if (q.isBlank()) flowOf(emptyList()) else repo.search(q)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()
    data class TimedLyric(val timeMs: Long, val text: String)

    private val _lyrics = MutableStateFlow<String?>(null)
    val lyrics = _lyrics.asStateFlow()
    private val _timedLyrics = MutableStateFlow<List<TimedLyric>>(emptyList())
    val timedLyrics = _timedLyrics.asStateFlow()

    init {
        player.connect()
        viewModelScope.launch {
            settings.settings.collect { cfg ->
                player.configureTransitionFade(cfg.crossfade, cfg.crossfadeMs)
            }
        }
        viewModelScope.launch {
            while (true) {
                player.updatePosition()
                delay(400)
            }
        }
        viewModelScope.launch {
            delay(800)
            if (appSettings.value.autoScan && songs.value.isEmpty()) scanLibrary()
        }
    }

    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun scanLibrary() {
        viewModelScope.launch {
            _isScanning.value = true
            repo.scanLibrary()
            _isScanning.value = false
        }
    }

    fun playSong(song: Song, list: List<Song> = songs.value) {
        val idx = list.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        player.playSongs(list, idx, appSettings.value.autoPlay)
        _lyrics.value = null
        _timedLyrics.value = emptyList()
    }
    fun playPlaylist(list: List<Song>, shuffle: Boolean = false) {
        if (list.isNotEmpty()) {
            player.playSongs(list, 0, appSettings.value.autoPlay, shuffle)
            _lyrics.value = null
            _timedLyrics.value = emptyList()
        }
    }
    fun songsInFolder(path: String): List<Song> = songs.value.filter { it.path.startsWith(path) }
    fun toggleFavorite(song: Song) = viewModelScope.launch { repo.setFavorite(song.id, !song.isFavorite) }

    fun createPlaylist(name: String) = viewModelScope.launch { if (name.isNotBlank()) repo.createPlaylist(name.trim()) }
    fun renamePlaylist(playlist: Playlist, name: String) = viewModelScope.launch {
        if (name.isNotBlank()) repo.renamePlaylist(playlist.copy(name = name.trim()))
    }
    fun deletePlaylist(playlist: Playlist) = viewModelScope.launch { repo.deletePlaylist(playlist) }
    fun addToPlaylist(playlistId: Long, songId: Long) = viewModelScope.launch { repo.addToPlaylist(playlistId, songId) }
    fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) = viewModelScope.launch { repo.addSongsToPlaylist(playlistId, songIds) }
    fun removeFromPlaylist(playlistId: Long, songId: Long) = viewModelScope.launch { repo.removeFromPlaylist(playlistId, songId) }
    fun getPlaylistSongs(id: Long): Flow<List<Song>> = repo.getPlaylistSongs(id)
    fun shufflePlaylist(id: Long) = viewModelScope.launch { repo.getPlaylistSongs(id).first().let { if (it.isNotEmpty()) playPlaylist(it, shuffle = true) } }

    fun setTheme(v: ThemeMode) = viewModelScope.launch { settings.setTheme(v) }
    fun setAccent(v: AccentPreset) = viewModelScope.launch { settings.setAccent(v) }
    fun setDensity(v: UiDensity) = viewModelScope.launch { settings.setDensity(v) }
    fun setCornerRadius(v: Int) = viewModelScope.launch { settings.setCornerRadius(v) }
    fun setLibrarySort(v: LibrarySort) = viewModelScope.launch { settings.setLibrarySort(v) }
    fun setScanSubfolders(v: Boolean) = viewModelScope.launch { settings.setScanSubfolders(v) }
    fun setAutoScan(v: Boolean) = viewModelScope.launch { settings.setAutoScan(v) }
    fun setHomeRecently(v: Boolean) = viewModelScope.launch { settings.setRecently(v) }
    fun setHomeFavorites(v: Boolean) = viewModelScope.launch { settings.setFavorites(v) }
    fun setHomePlaylists(v: Boolean) = viewModelScope.launch { settings.setPlaylists(v) }
    fun setHomeArtists(v: Boolean) = viewModelScope.launch { settings.setArtists(v) }
    fun setHomeAlbums(v: Boolean) = viewModelScope.launch { settings.setAlbums(v) }
    fun setHomeFolders(v: Boolean) = viewModelScope.launch { settings.setFolders(v) }
    fun setGapless(v: Boolean) = viewModelScope.launch { settings.setGapless(v) }
    fun setCrossfade(v: Boolean) = viewModelScope.launch { settings.setCrossfade(v) }
    fun setCrossfadeMs(v: Int) = viewModelScope.launch { settings.setCrossfadeMs(v) }
    fun setAutoPlay(v: Boolean) = viewModelScope.launch { settings.setAutoPlay(v) }
    fun setResume(v: Boolean) = viewModelScope.launch { settings.setResume(v) }
    fun setPauseNoisy(v: Boolean) = viewModelScope.launch { settings.setPauseNoisy(v) }
    fun setShowCodec(v: Boolean) = viewModelScope.launch { settings.setShowCodec(v) }
    fun setShowArt(v: Boolean) = viewModelScope.launch { settings.setShowArt(v) }

    fun addIncludePath(path: String) = viewModelScope.launch { settings.addIncludePath(path) }
    fun removeIncludePath(path: String) = viewModelScope.launch { settings.removeIncludePath(path) }
    fun addExcludePath(path: String) = viewModelScope.launch { settings.addExcludePath(path) }
    fun removeExcludePath(path: String) = viewModelScope.launch { settings.removeExcludePath(path) }
    fun clearLibraryPaths() = viewModelScope.launch { settings.clearLibraryPaths(); scanLibrary() }
    fun resetSettings() = viewModelScope.launch { settings.reset() }

    fun loadLyricsPlaceholder(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = readEmbeddedLyrics(song.uri)
            _lyrics.value = result?.first
            _timedLyrics.value = result?.second ?: emptyList()
        }
    }

    private fun readEmbeddedLyrics(uri: String): Pair<String, List<TimedLyric>>? {
        return try {
            val retriever = MediaMetadataRetriever()
            val pfd = getApplication<Application>().contentResolver.openFileDescriptor(Uri.parse(uri), "r")
            if (pfd == null) {
                retriever.release()
                return null
            }
            val raw = pfd.use {
                retriever.setDataSource(it.fileDescriptor)
                null
            }
            retriever.release()
            if (raw.isNullOrBlank()) return null

            val timed = parseTimedLyrics(raw)
            val clean = if (timed.isNotEmpty()) timed.joinToString("\n") { it.text } else raw.trim()
            clean to timed
        } catch (_: Exception) {
            null
        }
    }

    private fun parseTimedLyrics(raw: String): List<TimedLyric> {
        val regex = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?\\](.*)")
        val result = mutableListOf<TimedLyric>()
        raw.lineSequence().forEach { line ->
            regex.findAll(line).forEach { match ->
                val minutes = match.groupValues[1].toLongOrNull()
                val seconds = match.groupValues[2].toLongOrNull()
                if (minutes != null && seconds != null) {
                    val fraction = match.groupValues[3]
                    val fractionMs = when (fraction.length) {
                        1 -> (fraction.toLongOrNull() ?: 0L) * 100
                        2 -> (fraction.toLongOrNull() ?: 0L) * 10
                        3 -> fraction.toLongOrNull() ?: 0L
                        else -> 0L
                    }
                    val text = match.groupValues[4].trim()
                    if (text.isNotEmpty()) {
                        result += TimedLyric(
                            minutes * 60_000 + seconds * 1_000 + fractionMs,
                            text
                        )
                    }
                }
            }
        }
        return result.sortedBy { it.timeMs }
    }

    override fun onCleared() { player.release(); player.disconnect(); super.onCleared() }
}
