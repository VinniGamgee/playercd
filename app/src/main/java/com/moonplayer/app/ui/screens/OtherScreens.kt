package com.moonplayer.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moonplayer.app.data.model.Song
import com.moonplayer.app.ui.components.SongListItem
import com.moonplayer.app.ui.components.formatDuration
import com.moonplayer.app.ui.components.formatSize
import com.moonplayer.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(vm: MainViewModel, onBack: () -> Unit) {
    val queue by vm.player.queue.collectAsState()
    val current by vm.player.currentSong.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fila (${queue.size})") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { vm.player.clearQueue() }) {
                        Icon(Icons.Filled.ClearAll, "Limpar")
                    }
                }
            )
        }
    ) { padding ->
        if (queue.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Fila vazia")
            }
        } else {
            LazyColumn(Modifier.padding(padding)) {
                itemsIndexed(queue, key = { _, s -> s.id }) { index, song ->
                    val isCurrent = song.id == current?.id
                    ListItem(
                        headlineContent = {
                            Text(song.title, color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        },
                        supportingContent = { Text(song.artist) },
                        trailingContent = {
                            IconButton(onClick = { vm.player.removeFromQueue(index) }) {
                                Icon(Icons.Filled.Close, "Remover")
                            }
                        },
                        modifier = Modifier.clickable { vm.playSong(song, queue) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(vm: MainViewModel, onSongClick: (Song, List<Song>) -> Unit) {
    val query by vm.searchQuery.collectAsState()
    val results by vm.searchResults.collectAsState()

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { vm.setSearchQuery(it) },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text("Pesquisar músicas, artistas…") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { vm.setSearchQuery("") }) {
                    Icon(Icons.Filled.Clear, null)
                }
            },
            singleLine = true
        )
        if (query.isBlank()) {
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Digite para pesquisar", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (results.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Nenhum resultado")
            }
        } else {
            LazyColumn {
                items(results, key = { it.id }) { song ->
                    SongListItem(song = song, onClick = { onSongClick(song, results) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel) {
    val isScanning by vm.isScanning.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text("Configurações") }) }) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            item {
                Text("Interface", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary)
                ListItem(headlineContent = { Text("Tema") }, supportingContent = { Text("Seguir sistema") }, leadingContent = { Icon(Icons.Filled.DarkMode, null) })
            }
            item {
                Text("Reprodução", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary)
                ListItem(headlineContent = { Text("Gapless") }, supportingContent = { Text("Quando suportado") }, leadingContent = { Icon(Icons.Filled.GraphicEq, null) })
                ListItem(headlineContent = { Text("Crossfade") }, supportingContent = { Text("Desativado") }, leadingContent = { Icon(Icons.Filled.Tune, null) })
            }
            item {
                Text("Áudio", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary)
                ListItem(headlineContent = { Text("Equalizador") }, supportingContent = { Text("Em breve") }, leadingContent = { Icon(Icons.Filled.Equalizer, null) })
            }
            item {
                Text("Biblioteca", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary)
                ListItem(
                    headlineContent = { Text("Atualizar biblioteca") },
                    supportingContent = { Text(if (isScanning) "Escaneando…" else "Escanear músicas do dispositivo") },
                    leadingContent = { Icon(Icons.Filled.Refresh, null) },
                    modifier = Modifier.clickable { vm.scanLibrary() }
                )
            }
            item {
                Text("Sobre", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary)
                ListItem(headlineContent = { Text("MoonPlayer") }, supportingContent = { Text("Versão 1.0.0") }, leadingContent = { Icon(Icons.Filled.Info, null) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongInfoScreen(vm: MainViewModel, songId: Long, onBack: () -> Unit) {
    val songs by vm.songs.collectAsState()
    val song = songs.find { it.id == songId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Informações") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        if (song == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Música não encontrada")
            }
        } else {
            LazyColumn(Modifier.padding(padding).padding(16.dp)) {
                item { InfoRow("Título", song.title) }
                item { InfoRow("Artista", song.artist) }
                item { InfoRow("Álbum", song.album) }
                item { InfoRow("Duração", formatDuration(song.duration)) }
                item { InfoRow("Formato", song.format) }
                item { InfoRow("Tamanho", formatSize(song.size)) }
                if (song.year > 0) item { InfoRow("Ano", song.year.toString()) }
                if (song.genre.isNotBlank()) item { InfoRow("Gênero", song.genre) }
                item { InfoRow("Caminho", song.path) }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(vm: MainViewModel, artist: String, onBack: () -> Unit, onSongClick: (Song, List<Song>) -> Unit) {
    val songs by vm.songs.collectAsState()
    val list = songs.filter { it.artist == artist }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(artist) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } })
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            items(list, key = { it.id }) { song ->
                SongListItem(song = song, onClick = { onSongClick(song, list) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(vm: MainViewModel, albumId: Long, onBack: () -> Unit, onSongClick: (Song, List<Song>) -> Unit) {
    val songs by vm.songs.collectAsState()
    val list = songs.filter { it.albumId == albumId }.sortedBy { it.trackNumber }
    val albumName = list.firstOrNull()?.album ?: "Álbum"
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(albumName) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } })
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            items(list, key = { it.id }) { song ->
                SongListItem(song = song, onClick = { onSongClick(song, list) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(vm: MainViewModel, playlistId: Long, onBack: () -> Unit, onSongClick: (Song, List<Song>) -> Unit) {
    val list by vm.getPlaylistSongs(playlistId).collectAsState(initial = emptyList())
    val playlists by vm.playlists.collectAsState()
    val name = playlists.find { it.id == playlistId }?.name ?: "Playlist"
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(name) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
                actions = {
                    if (list.isNotEmpty()) {
                        IconButton(onClick = { vm.playPlaylist(list) }) {
                            Icon(Icons.Filled.PlayArrow, "Reproduzir")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (list.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Playlist vazia")
            }
        } else {
            LazyColumn(Modifier.padding(padding)) {
                items(list, key = { it.id }) { song ->
                    SongListItem(song = song, onClick = { onSongClick(song, list) })
                }
            }
        }
    }
}
