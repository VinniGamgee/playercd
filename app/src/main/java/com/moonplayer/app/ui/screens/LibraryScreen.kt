package com.moonplayer.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moonplayer.app.data.model.Song
import com.moonplayer.app.ui.components.SongListItem
import com.moonplayer.app.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    vm: MainViewModel,
    onSongClick: (Song, List<Song>) -> Unit,
    onArtistClick: (String) -> Unit,
    onAlbumClick: (Long) -> Unit,
    onPlaylistClick: (Long) -> Unit,
    onFolderClick: (String) -> Unit,
    onSongInfo: (Long) -> Unit
) {
    val tabs = listOf("Músicas", "Artistas", "Álbuns", "Pastas", "Playlists", "Favoritos")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    val songs by vm.songs.collectAsState()
    val artists by vm.artists.collectAsState()
    val albums by vm.albums.collectAsState()
    val folders by vm.folders.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val isScanning by vm.isScanning.collectAsState()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("MoonPlayer")
                    if (songs.isNotEmpty()) {
                        Text(
                            "${songs.size} músicas",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            actions = {
                IconButton(onClick = { vm.scanLibrary() }) {
                    if (isScanning) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Refresh, "Atualizar biblioteca")
                    }
                }
            }
        )
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            edgePadding = 8.dp,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            tabs.forEachIndexed { i, title ->
                Tab(
                    selected = pagerState.currentPage == i,
                    onClick = { scope.launch { pagerState.animateScrollToPage(i) } },
                    text = { Text(title) }
                )
            }
        }
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            when (page) {
                0 -> SongList(songs, onSongClick, { vm.toggleFavorite(it) }, onSongInfo)
                1 -> {
                    if (artists.isEmpty()) EmptyState("Nenhum artista")
                    else LazyColumn {
                        items(artists, key = { it.name }) { a ->
                            ListItem(
                                headlineContent = { Text(a.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                supportingContent = {
                                    Text("${a.songCount} músicas • ${a.albumCount} álbuns")
                                },
                                leadingContent = {
                                    Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.primary)
                                },
                                modifier = Modifier.clickable { onArtistClick(a.name) }
                            )
                        }
                    }
                }
                2 -> {
                    if (albums.isEmpty()) EmptyState("Nenhum álbum")
                    else LazyColumn {
                        items(albums, key = { it.id }) { a ->
                            ListItem(
                                headlineContent = { Text(a.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                supportingContent = {
                                    Text("${a.artist} • ${a.songCount} músicas")
                                },
                                leadingContent = {
                                    Icon(Icons.Filled.Album, null, tint = MaterialTheme.colorScheme.primary)
                                },
                                modifier = Modifier.clickable { onAlbumClick(a.id) }
                            )
                        }
                    }
                }
                3 -> {
                    if (folders.isEmpty()) EmptyState("Nenhuma pasta")
                    else LazyColumn {
                        items(folders, key = { it.path }) { f ->
                            ListItem(
                                headlineContent = { Text(f.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                supportingContent = {
                                    Text("${f.songCount} músicas • ${f.path}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                },
                                leadingContent = {
                                    Icon(Icons.Filled.Folder, null, tint = MaterialTheme.colorScheme.primary)
                                },
                                trailingContent = {
                                    Icon(Icons.Filled.ChevronRight, null)
                                },
                                modifier = Modifier.clickable { onFolderClick(f.path) }
                            )
                        }
                    }
                }
                4 -> {
                    var showCreate by remember { mutableStateOf(false) }
                    Column {
                        TextButton(
                            onClick = { showCreate = true },
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(Icons.Filled.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Nova playlist")
                        }
                        if (playlists.isEmpty()) EmptyState("Nenhuma playlist")
                        else LazyColumn {
                            items(playlists, key = { it.id }) { p ->
                                ListItem(
                                    headlineContent = { Text(p.name) },
                                    leadingContent = {
                                        Icon(Icons.Filled.QueueMusic, null, tint = MaterialTheme.colorScheme.primary)
                                    },
                                    trailingContent = { Icon(Icons.Filled.ChevronRight, null) },
                                    modifier = Modifier.clickable { onPlaylistClick(p.id) }
                                )
                            }
                        }
                    }
                    if (showCreate) {
                        var name by remember { mutableStateOf("") }
                        AlertDialog(
                            onDismissRequest = { showCreate = false },
                            title = { Text("Nova playlist") },
                            text = {
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Nome") },
                                    singleLine = true
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    if (name.isNotBlank()) {
                                        vm.createPlaylist(name)
                                        showCreate = false
                                    }
                                }) { Text("Criar") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCreate = false }) { Text("Cancelar") }
                            }
                        )
                    }
                }
                5 -> SongList(favorites, onSongClick, { vm.toggleFavorite(it) }, onSongInfo)
            }
        }
    }
}

@Composable
private fun SongList(
    songs: List<Song>,
    onSongClick: (Song, List<Song>) -> Unit,
    onFavorite: (Song) -> Unit,
    onSongInfo: (Long) -> Unit
) {
    if (songs.isEmpty()) {
        EmptyState("Nenhuma música encontrada\nToque em ↻ para escanear")
    } else {
        LazyColumn {
            items(songs, key = { it.id }) { song ->
                SongListItem(
                    song = song,
                    onClick = { onSongClick(song, songs) },
                    onFavorite = { onFavorite(song) },
                    onMore = { onSongInfo(song.id) }
                )
            }
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
