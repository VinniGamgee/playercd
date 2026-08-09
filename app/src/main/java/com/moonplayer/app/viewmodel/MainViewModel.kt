package com.moonplayer.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.moonplayer.app.data.db.AppDatabase
import com.moonplayer.app.data.model.*
import com.moonplayer.app.data.preferences.SettingsRepository
import com.moonplayer.app.data.preferences.ThemeMode
import com.moonplayer.app.data.repository.MusicRepository
import com.moonplayer.app.player.PlayerManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repo = MusicRepository(application, db.songDao(), db.playlistDao())
    private val settings = SettingsRepository(application)
    val player = PlayerManager(application)

    val songs = repo.getAllSongs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val favorites = repo.getFavorites().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val artists = repo.getArtists().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val albums = repo.getAlbums().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val folders = repo.getFolders().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val playlists = repo.getPlaylists().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val themeMode = settings.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)
    val gapless = settings.gapless.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val crossfade = settings.crossfade.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val autoPlay = settings.autoPlay.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    val searchResults = _searchQuery.flatMapLatest { q ->
        if (q.isBlank()) flowOf(emptyList()) else repo.search(q)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _lyrics = MutableStateFlow<String?>(null)
    val lyrics = _lyrics.asStateFlow()

    init {
        player.connect()
        viewModelScope.launch {
            while (true) {
                player.updatePosition()
                delay(400)
            }
        }
        // Auto-scan once if library empty
        viewModelScope.launch {
            delay(800)
            if (songs.value.isEmpty()) scanLibrary()
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
        player.playSongs(list, idx)
        _lyrics.value = null
    }

    fun playPlaylist(list: List<Song>) {
        if (list.isNotEmpty()) {
            player.playSongs(list, 0)
            _lyrics.value = null
        }
    }

    fun songsInFolder(path: String): List<Song> =
        songs.value.filter { it.path.startsWith(path) }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch { repo.setFavorite(song.id, !song.isFavorite) }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch { repo.createPlaylist(name) }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch { repo.deletePlaylist(playlist) }
    }

    fun addToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch { repo.addToPlaylist(playlistId, songId) }
    }

    fun getPlaylistSongs(id: Long): Flow<List<Song>> = repo.getPlaylistSongs(id)

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settings.setTheme(mode) }
    }

    fun setGapless(v: Boolean) {
        viewModelScope.launch { settings.setGapless(v) }
    }

    fun setCrossfade(v: Boolean) {
        viewModelScope.launch { settings.setCrossfade(v) }
    }

    fun setAutoPlay(v: Boolean) {
        viewModelScope.launch { settings.setAutoPlay(v) }
    }

    fun loadLyricsPlaceholder(song: Song) {
        _lyrics.value = buildString {
            appendLine("♪ ${song.title}")
            appendLine("  ${song.artist}")
            appendLine()
            appendLine("Letras embutidas não encontradas neste arquivo.")
            appendLine()
            appendLine("Em versões futuras o MoonPlayer buscará")
            appendLine("letras sincronizadas (LRC) e online.")
            appendLine()
            appendLine("Formato: ${song.format}")
            if (song.duration > 0) appendLine("Duração: ${song.duration / 1000}s")
        }
    }

    override fun onCleared() {
        player.disconnect()
        super.onCleared()
    }
}
