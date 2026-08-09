package com.moonplayer.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.moonplayer.app.data.db.AppDatabase
import com.moonplayer.app.data.model.*
import com.moonplayer.app.data.repository.MusicRepository
import com.moonplayer.app.player.PlayerManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repo = MusicRepository(application, db.songDao(), db.playlistDao())
    val player = PlayerManager(application)

    val songs = repo.getAllSongs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val favorites = repo.getFavorites().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val artists = repo.getArtists().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val albums = repo.getAlbums().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val folders = repo.getFolders().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val playlists = repo.getPlaylists().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    val searchResults = _searchQuery.flatMapLatest { q ->
        if (q.isBlank()) flowOf(emptyList()) else repo.search(q)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab = _selectedTab.asStateFlow()

    init {
        player.connect()
        viewModelScope.launch {
            while (true) {
                player.updatePosition()
                delay(500)
            }
        }
    }

    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun selectTab(i: Int) { _selectedTab.value = i }

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
    }

    fun playPlaylist(list: List<Song>) {
        if (list.isNotEmpty()) player.playSongs(list, 0)
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            repo.setFavorite(song.id, !song.isFavorite)
        }
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

    override fun onCleared() {
        player.disconnect()
        super.onCleared()
    }
}
