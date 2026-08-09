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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moonplayer.app.data.model.Song
import com.moonplayer.app.data.preferences.ThemeMode
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
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) }
                },
                actions = {
                    if (queue.isNotEmpty()) {
                        IconButton(onClick = { vm.player.clearQueue() }) {
                            Icon(Icons.Filled.ClearAll, "Limpar")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (queue.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { Text("Fila vazia") }
        } else {
            LazyColumn(Modifier.padding(padding)) {
                itemsIndexed(queue, key = { _, s -> s.id }) { index, song ->
                    val isCurrent = song.id == current?.id
                    ListItem(
                        headlineContent = {
                            Text(
                                song.title,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        supportingContent = { Text(song.artist, maxLines = 1) },
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
                if (query.isNotEmpty()) {
                    IconButton(onClick = { vm.setSearchQuery("") }) {
                        Icon(Icons.Filled.Clear, null)
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.large
        )
        when {
            query.isBlank() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Digite para pesquisar", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            results.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhum resultado")
            }
            else -> LazyColumn {
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
    val themeMode by vm.themeMode.collectAsState()
    val gapless by vm.gapless.collectAsState()
    val crossfade by vm.crossfade.collectAsState()
    val autoPlay by vm.autoPlay.collectAsState()
    val songCount by vm.songs.collectAsState()

    var showThemeDialog by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Configurações") }) }) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            item {
                SectionHeader("Interface")
                ListItem(
                    headlineContent = { Text("Tema") },
                    supportingContent = {
                        Text(
                            when (themeMode) {
                                ThemeMode.SYSTEM -> "Seguir sistema"
                                ThemeMode.LIGHT -> "Claro"
                                ThemeMode.DARK -> "Escuro"
                            }
                        )
                    },
                    leadingContent = { Icon(Icons.Filled.DarkMode, null) },
                    trailingContent = { Icon(Icons.Filled.ChevronRight, null) },
                    modifier = Modifier.clickable { showThemeDialog = true }
                )
            }
            item {
                SectionHeader("Reprodução")
                ListItem(
                    headlineContent = { Text("Reprodução automática") },
                    supportingContent = { Text("Continuar ao abrir o app") },
                    leadingContent = { Icon(Icons.Filled.PlayCircle, null) },
                    trailingContent = {
                        Switch(checked = autoPlay, onCheckedChange = { vm.setAutoPlay(it) })
                    }
                )
                ListItem(
                    headlineContent = { Text("Gapless") },
                    supportingContent = { Text("Transição sem silêncio") },
                    leadingContent = { Icon(Icons.Filled.GraphicEq, null) },
                    trailingContent = {
                        Switch(checked = gapless, onCheckedChange = { vm.setGapless(it) })
                    }
                )
                ListItem(
                    headlineContent = { Text("Crossfade") },
                    supportingContent = { Text("Sobrepor faixas na troca") },
                    leadingContent = { Icon(Icons.Filled.Tune, null) },
                    trailingContent = {
                        Switch(checked = crossfade, onCheckedChange = { vm.setCrossfade(it) })
                    }
                )
            }
            item {
                SectionHeader("Áudio")
                ListItem(
                    headlineContent = { Text("Equalizador") },
                    supportingContent = { Text("Use o equalizador do sistema") },
                    leadingContent = { Icon(Icons.Filled.Equalizer, null) },
                    modifier = Modifier.clickable {
                        // Opens system EQ if available via intent from host activity in future
                    }
                )
            }
            item {
                SectionHeader("Biblioteca")
                ListItem(
                    headlineContent = { Text("Atualizar biblioteca") },
                    supportingContent = {
                        Text(
                            if (isScanning) "Escaneando…"
                            else "${songCount.size} músicas no dispositivo"
                        )
                    },
                    leadingContent = {
                        if (isScanning) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Filled.Refresh, null)
                    },
                    modifier = Modifier.clickable(enabled = !isScanning) { vm.scanLibrary() }
                )
            }
            item {
                SectionHeader("Sobre")
                ListItem(
                    headlineContent = { Text("MoonPlayer") },
                    supportingContent = { Text("Versão 1.1.0 • Kotlin + Compose + Media3") },
                    leadingContent = { Icon(Icons.Filled.Info, null) }
                )
            }
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Tema") },
            text = {
                Column {
                    ThemeOption("Seguir sistema", themeMode == ThemeMode.SYSTEM) {
                        vm.setTheme(ThemeMode.SYSTEM)
                        showThemeDialog = false
                    }
                    ThemeOption("Claro", themeMode == ThemeMode.LIGHT) {
                        vm.setTheme(ThemeMode.LIGHT)
                        showThemeDialog = false
                    }
                    ThemeOption("Escuro", themeMode == ThemeMode.DARK) {
                        vm.setTheme(ThemeMode.DARK)
                        showThemeDialog = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Fechar") }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = {
            if (selected) Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary)
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
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
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        if (song == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
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
    Column(Modifier.padding(vertical = 10.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    vm: MainViewModel,
    artist: String,
    onBack: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit
) {
    val songs by vm.songs.collectAsState()
    val list = remember(songs, artist) { songs.filter { it.artist == artist } }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(artist) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) }
                },
                actions = {
                    if (list.isNotEmpty()) {
                        IconButton(onClick = { vm.playPlaylist(list) }) {
                            Icon(Icons.Filled.PlayArrow, "Tocar tudo")
                        }
                    }
                }
            )
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
fun AlbumDetailScreen(
    vm: MainViewModel,
    albumId: Long,
    onBack: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit
) {
    val songs by vm.songs.collectAsState()
    val list = remember(songs, albumId) {
        songs.filter { it.albumId == albumId }.sortedBy { it.trackNumber }
    }
    val albumName = list.firstOrNull()?.album ?: "Álbum"
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(albumName) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) }
                },
                actions = {
                    if (list.isNotEmpty()) {
                        IconButton(onClick = { vm.playPlaylist(list) }) {
                            Icon(Icons.Filled.PlayArrow, "Tocar tudo")
                        }
                    }
                }
            )
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
fun PlaylistDetailScreen(
    vm: MainViewModel,
    playlistId: Long,
    onBack: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit
) {
    val list by vm.getPlaylistSongs(playlistId).collectAsState(initial = emptyList())
    val playlists by vm.playlists.collectAsState()
    val name = playlists.find { it.id == playlistId }?.name ?: "Playlist"
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(name) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) }
                },
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
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    vm: MainViewModel,
    folderPath: String,
    onBack: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit
) {
    val allSongs by vm.songs.collectAsState()
    val list = remember(folderPath, allSongs) {
        allSongs.filter { it.path.startsWith(folderPath) }
    }
    val folderName = folderPath.substringAfterLast('/')

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(folderName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            folderPath,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) }
                },
                actions = {
                    if (list.isNotEmpty()) {
                        IconButton(onClick = { vm.playPlaylist(list) }) {
                            Icon(Icons.Filled.PlayArrow, "Tocar pasta")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (list.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Nenhuma música nesta pasta")
            }
        } else {
            LazyColumn(Modifier.padding(padding)) {
                item {
                    Text(
                        "${list.size} músicas",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                items(list, key = { it.id }) { song ->
                    SongListItem(song = song, onClick = { onSongClick(song, list) })
                }
            }
        }
    }
}
