package com.moonplayer.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "MoonPlayer",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (songs.isEmpty()) "Sua música, do seu jeito."
                        else "${songs.size} músicas na biblioteca",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalIconButton(
                    onClick = { if (!isScanning) vm.scanLibrary() }
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Filled.Refresh, "Atualizar biblioteca")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LibraryQuickAction(
                    icon = Icons.Filled.MusicNote,
                    label = "Músicas",
                    value = songs.size.toString(),
                    modifier = Modifier.weight(1f)
                )
                LibraryQuickAction(
                    icon = Icons.Filled.Person,
                    label = "Artistas",
                    value = artists.size.toString(),
                    modifier = Modifier.weight(1f)
                )
                LibraryQuickAction(
                    icon = Icons.Filled.Favorite,
                    label = "Favoritos",
                    value = favorites.size.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            edgePadding = 20.dp,
            containerColor = MaterialTheme.colorScheme.background,
            divider = {}
        ) {
            tabs.forEachIndexed { i, title ->
                Tab(
                    selected = pagerState.currentPage == i,
                    onClick = { scope.launch { pagerState.animateScrollToPage(i) } },
                    text = {
                        Text(
                            title,
                            fontWeight = if (pagerState.currentPage == i)
                                FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> SongList(songs, onSongClick, { vm.toggleFavorite(it) }, onSongInfo)

                1 -> {
                    if (artists.isEmpty()) {
                        EmptyState("Nenhum artista encontrado")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(artists, key = { it.name }) { a ->
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            a.name,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    supportingContent = {
                                        Text("${a.songCount} músicas • ${a.albumCount} álbuns")
                                    },
                                    leadingContent = {
                                        LibraryIcon(Icons.Filled.Person)
                                    },
                                    trailingContent = {
                                        Icon(Icons.Filled.ChevronRight, null)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onArtistClick(a.name) }
                                )
                            }
                        }
                    }
                }

                2 -> {
                    if (albums.isEmpty()) {
                        EmptyState("Nenhum álbum encontrado")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(albums, key = { it.id }) { a ->
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            a.name,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    supportingContent = {
                                        Text("${a.artist} • ${a.songCount} músicas")
                                    },
                                    leadingContent = {
                                        LibraryIcon(Icons.Filled.Album)
                                    },
                                    trailingContent = {
                                        Icon(Icons.Filled.ChevronRight, null)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onAlbumClick(a.id) }
                                )
                            }
                        }
                    }
                }

                3 -> {
                    if (folders.isEmpty()) {
                        EmptyState("Nenhuma pasta encontrada")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(folders, key = { it.path }) { f ->
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            f.name,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            "${f.songCount} músicas • ${f.path}",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    leadingContent = {
                                        LibraryIcon(Icons.Filled.Folder)
                                    },
                                    trailingContent = {
                                        Icon(Icons.Filled.ChevronRight, null)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onFolderClick(f.path) }
                                )
                            }
                        }
                    }
                }

                4 -> PlaylistTab(
                    playlists = playlists,
                    vm = vm,
                    onPlaylistClick = onPlaylistClick
                )

                5 -> SongList(
                    favorites,
                    onSongClick,
                    { vm.toggleFavorite(it) },
                    onSongInfo
                )
            }
        }
    }
}

@Composable
private fun LibraryQuickAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LibraryIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun PlaylistTab(
    playlists: List<com.moonplayer.app.data.model.Playlist>,
    vm: MainViewModel,
    onPlaylistClick: (Long) -> Unit
) {
    var showCreate by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Suas playlists",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            FilledTonalButton(onClick = { showCreate = true }) {
                Icon(Icons.Filled.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("Nova")
            }
        }

        if (playlists.isEmpty()) {
            EmptyState("Nenhuma playlist ainda")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(playlists, key = { it.id }) { p ->
                    ListItem(
                        headlineContent = { Text(p.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text("Toque para abrir") },
                        leadingContent = { LibraryIcon(Icons.Filled.QueueMusic) },
                        trailingContent = { Icon(Icons.Filled.ChevronRight, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlaylistClick(p.id) }
                    )
                }
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = {
                showCreate = false
                name = ""
            },
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
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            vm.createPlaylist(name.trim())
                            showCreate = false
                            name = ""
                        }
                    }
                ) { Text("Criar") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreate = false
                        name = ""
                    }
                ) { Text("Cancelar") }
            }
        )
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
        LazyColumn(
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
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
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
