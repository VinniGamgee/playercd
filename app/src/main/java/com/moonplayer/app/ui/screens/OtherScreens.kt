package com.moonplayer.app.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moonplayer.app.data.model.Song
import com.moonplayer.app.data.preferences.*
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
    val context = LocalContext.current
    val cfg by vm.appSettings.collectAsState()
    val includes by vm.includePaths.collectAsState()
    val excludes by vm.excludePaths.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val isScanning by vm.isScanning.collectAsState()
    val songCount by vm.songs.collectAsState()

    var dialog by remember { mutableStateOf<String?>(null) }
    var showReset by remember { mutableStateOf(false) }
    var showCreatePlaylist by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("") }

    val includePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        vm.addIncludePath(treeUriToPath(uri))
    }
    val excludePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        vm.addExcludePath(treeUriToPath(uri))
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Configurações") }) }) { padding ->
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                SectionHeader("Aparência")
                SettingsChoice("Tema", when (cfg.theme) {
                    ThemeMode.SYSTEM -> "Sistema"
                    ThemeMode.LIGHT -> "Claro"
                    ThemeMode.DARK -> "Escuro"
                    ThemeMode.AMOLED -> "AMOLED"
                }, Icons.Filled.DarkMode) { dialog = "theme" }
                SettingsChoice("Cor de destaque", accentLabel(cfg.accent), Icons.Filled.Palette) { dialog = "accent" }
                SettingsChoice("Densidade", if (cfg.density == UiDensity.COMPACT) "Compacta" else "Confortável", Icons.Filled.ViewCompact) { dialog = "density" }
                SettingsChoice("Arredondamento", "${cfg.cornerRadius} dp", Icons.Filled.CropSquare) { dialog = "corner" }
                SettingsSwitch("Mostrar capas", "Usar arte do álbum sempre que disponível", Icons.Filled.Image, cfg.showAlbumArt, vm::setShowArt)
            }
            item {
                SectionHeader("Tela inicial")
                SettingsSwitch("Tocadas recentemente", "Mostrar seção de músicas recentes", Icons.Filled.History, cfg.showRecentlyPlayed, vm::setHomeRecently)
                SettingsSwitch("Favoritos", "Mostrar favoritos na tela inicial", Icons.Filled.Favorite, cfg.showFavorites, vm::setHomeFavorites)
                SettingsSwitch("Playlists", "Mostrar suas playlists", Icons.Filled.QueueMusic, cfg.showPlaylists, vm::setHomePlaylists)
                SettingsSwitch("Artistas", "Mostrar artistas", Icons.Filled.Person, cfg.showArtists, vm::setHomeArtists)
                SettingsSwitch("Álbuns", "Mostrar álbuns", Icons.Filled.Album, cfg.showAlbums, vm::setHomeAlbums)
                SettingsSwitch("Pastas", "Mostrar pastas", Icons.Filled.Folder, cfg.showFolders, vm::setHomeFolders)
            }
            item {
                SectionHeader("Biblioteca")
                SettingsChoice("Ordenação", sortLabel(cfg.librarySort), Icons.Filled.Sort) { dialog = "sort" }
                SettingsSwitch("Escanear subpastas", "Inclui tudo dentro das pastas monitoradas", Icons.Filled.FolderOpen, cfg.scanSubfolders, vm::setScanSubfolders)
                SettingsSwitch("Atualização automática", "Escanear automaticamente quando a biblioteca estiver vazia", Icons.Filled.Autorenew, cfg.autoScan, vm::setAutoScan)
                SettingsChoice(
                    "Pastas monitoradas",
                    if (includes.isEmpty()) "Todo o armazenamento de músicas" else "${includes.size} pasta(s)",
                    Icons.Filled.LibraryMusic
                ) { dialog = "include" }
                SettingsChoice(
                    "Pastas ignoradas",
                    if (excludes.isEmpty()) "Nenhuma" else "${excludes.size} pasta(s)",
                    Icons.Filled.Block
                ) { dialog = "exclude" }
                ListItem(
                    headlineContent = { Text("Atualizar biblioteca") },
                    supportingContent = { Text(if (isScanning) "Escaneando…" else "${songCount.size} músicas encontradas") },
                    leadingContent = {
                        if (isScanning) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Filled.Refresh, null)
                    },
                    modifier = Modifier.clickable(enabled = !isScanning) { vm.scanLibrary() }
                )
            }
            item {
                SectionHeader("Reprodução")
                SettingsSwitch("Reprodução automática", "Iniciar reprodução quando uma fila for preparada", Icons.Filled.PlayCircle, cfg.autoPlay, vm::setAutoPlay)
                SettingsSwitch("Retomar posição", "Continuar a faixa a partir da última posição (preparação para persistência)", Icons.Filled.RestartAlt, cfg.resumePlayback, vm::setResume)
                SettingsSwitch("Gapless", "Evita pausas entre faixas quando suportado pelo decoder", Icons.Filled.GraphicEq, cfg.gapless, vm::setGapless)
                SettingsSwitch("Fade de transição", "Suaviza a entrada da próxima faixa", Icons.Filled.Tune, cfg.crossfade, vm::setCrossfade)
                if (cfg.crossfade) {
                    ListItem(
                        headlineContent = { Text("Duração do fade") },
                        supportingContent = { Text("${cfg.crossfadeMs} ms") },
                        leadingContent = { Icon(Icons.Filled.Timer, null) },
                        modifier = Modifier.clickable { dialog = "crossfade" }
                    )
                }
                SettingsSwitch("Pausar ao remover o fone", "Usa o comportamento de áudio do Android", Icons.Filled.HeadsetOff, cfg.pauseOnNoisy, vm::setPauseNoisy)
            }
            item {
                SectionHeader("Player")
                SettingsSwitch("Mostrar informações técnicas", "Codec, formato e metadados quando disponíveis", Icons.Filled.Info, cfg.showCodecInfo, vm::setShowCodec)
                ListItem(
                    headlineContent = { Text("Gestos e controles") },
                    supportingContent = { Text("Controles de reprodução ficam no Player e na notificação") },
                    leadingContent = { Icon(Icons.Filled.TouchApp, null) }
                )
            }
            item {
                SectionHeader("Playlists")
                ListItem(
                    headlineContent = { Text("Nova playlist") },
                    supportingContent = { Text("Crie uma playlist para organizar suas músicas") },
                    leadingContent = { Icon(Icons.Filled.Add, null) },
                    modifier = Modifier.clickable { showCreatePlaylist = true }
                )
                playlists.take(8).forEach { playlist ->
                    ListItem(
                        headlineContent = { Text(playlist.name) },
                        supportingContent = { Text("Playlist • toque para abrir") },
                        leadingContent = { Icon(Icons.Filled.QueueMusic, null) },
                        trailingContent = {
                            IconButton(onClick = { vm.deletePlaylist(playlist) }) {
                                Icon(Icons.Filled.DeleteOutline, "Excluir")
                            }
                        }
                    )
                }
            }
            item {
                SectionHeader("Avançado")
                ListItem(
                    headlineContent = { Text("Restaurar configurações") },
                    supportingContent = { Text("Volta todas as preferências aos valores padrão") },
                    leadingContent = { Icon(Icons.Filled.RestartAlt, null) },
                    modifier = Modifier.clickable { showReset = true }
                )
                ListItem(
                    headlineContent = { Text("MoonPlayer") },
                    supportingContent = { Text("Configurações 1.0 • Kotlin + Compose + Media3") },
                    leadingContent = { Icon(Icons.Filled.Info, null) }
                )
            }
        }
    }

    when (dialog) {
        "theme" -> ChoiceDialog("Tema", listOf(
            ThemeMode.SYSTEM to "Sistema", ThemeMode.LIGHT to "Claro", ThemeMode.DARK to "Escuro", ThemeMode.AMOLED to "AMOLED"
        ), cfg.theme, { vm.setTheme(it) }) { dialog = null }
        "accent" -> ChoiceDialog("Cor de destaque", AccentPreset.values().map { it to accentLabel(it) }, cfg.accent, { vm.setAccent(it) }) { dialog = null }
        "density" -> ChoiceDialog("Densidade", listOf(UiDensity.COMFORTABLE to "Confortável", UiDensity.COMPACT to "Compacta"), cfg.density, { vm.setDensity(it) }) { dialog = null }
        "sort" -> ChoiceDialog("Ordenar biblioteca", LibrarySort.values().map { it to sortLabel(it) }, cfg.librarySort, { vm.setLibrarySort(it) }) { dialog = null }
        "corner" -> NumberDialog("Arredondamento", cfg.cornerRadius, 4, 28, "dp", { vm.setCornerRadius(it) }) { dialog = null }
        "crossfade" -> NumberDialog("Duração do crossfade", cfg.crossfadeMs, 200, 3000, "ms", { vm.setCrossfadeMs(it) }) { dialog = null }
        "include" -> FolderRulesDialog(
            title = "Pastas monitoradas",
            paths = includes,
            addLabel = "Adicionar pasta",
            onAdd = { includePicker.launch(null) },
            onRemove = { path -> vm.removeIncludePath(path) },
            onClose = { dialog = null }
        )
        "exclude" -> FolderRulesDialog(
            title = "Pastas ignoradas",
            paths = excludes,
            addLabel = "Ignorar pasta",
            onAdd = { excludePicker.launch(null) },
            onRemove = { path -> vm.removeExcludePath(path) },
            onClose = { dialog = null }
        )
    }

    if (showCreatePlaylist) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylist = false; playlistName = "" },
            title = { Text("Nova playlist") },
            text = { OutlinedTextField(playlistName, { playlistName = it }, label = { Text("Nome") }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    vm.createPlaylist(playlistName)
                    showCreatePlaylist = false
                    playlistName = ""
                }) { Text("Criar") }
            },
            dismissButton = { TextButton(onClick = { showCreatePlaylist = false; playlistName = "" }) { Text("Cancelar") } }
        )
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("Restaurar configurações?") },
            text = { Text("Isso apaga suas preferências de aparência, biblioteca e reprodução. Suas músicas e playlists permanecem intactas.") },
            confirmButton = {
                TextButton(onClick = { vm.resetSettings(); showReset = false }) { Text("Restaurar") }
            },
            dismissButton = { TextButton(onClick = { showReset = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun SettingsChoice(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(value) },
        leadingContent = { Icon(icon, null) },
        trailingContent = { Icon(Icons.Filled.ChevronRight, null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun SettingsSwitch(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, onChecked: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, null) },
        trailingContent = { Switch(checked, onCheckedChange = onChecked) }
    )
}

@Composable
private fun <T> ChoiceDialog(title: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    ListItem(
                        headlineContent = { Text(label) },
                        trailingContent = { if (value == selected) Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable { onSelect(value); onClose() }
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Fechar") } }
    )
}

@Composable
private fun NumberDialog(title: String, initial: Int, min: Int, max: Int, unit: String, onValue: (Int) -> Unit, onClose: () -> Unit) {
    var value by remember(initial) { mutableFloatStateOf(initial.toFloat()) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(title) },
        text = {
            Column {
                Text("${value.toInt()} $unit", style = MaterialTheme.typography.titleMedium)
                Slider(value, { value = it }, valueRange = min.toFloat()..max.toFloat(), steps = ((max - min) / 100).coerceAtLeast(1) - 1)
            }
        },
        confirmButton = { TextButton(onClick = { onValue(value.toInt()); onClose() }) { Text("Aplicar") } },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancelar") } }
    )
}

@Composable
private fun FolderRulesDialog(
    title: String,
    paths: Set<String>,
    addLabel: String,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
    onClose: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(title) },
        text = {
            Column {
                if (paths.isEmpty()) Text("Nenhuma pasta definida.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                paths.sorted().forEach { path ->
                    ListItem(
                        headlineContent = { Text(path, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                        trailingContent = {
                            IconButton(onClick = { onRemove(path) }) { Icon(Icons.Filled.DeleteOutline, "Remover") }
                        }
                    )
                }
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.CreateNewFolder, null)
                    Spacer(Modifier.width(8.dp))
                    Text(addLabel)
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Fechar") } }
    )
}

private fun accentLabel(v: AccentPreset) = when (v) {
    AccentPreset.MOON -> "Moon • Azul celeste"
    AccentPreset.PURPLE -> "Nebula • Roxo"
    AccentPreset.GREEN -> "Forest • Verde"
    AccentPreset.ORANGE -> "Solar • Laranja"
    AccentPreset.PINK -> "Aurora • Rosa"
    AccentPreset.RED -> "Crimson • Vermelho"
}

private fun sortLabel(v: LibrarySort) = when (v) {
    LibrarySort.TITLE -> "Título"
    LibrarySort.ARTIST -> "Artista"
    LibrarySort.ALBUM -> "Álbum"
    LibrarySort.DATE_ADDED -> "Data adicionada"
    LibrarySort.DURATION -> "Duração"
}

private fun treeUriToPath(uri: android.net.Uri): String {
    val docId = android.provider.DocumentsContract.getTreeDocumentId(uri)
    val split = docId.split(":", limit = 2)
    return if (split.size == 2 && split[0].equals("primary", true)) {
        val root = android.os.Environment.getExternalStorageDirectory().absolutePath
        if (split[1].isBlank()) root else "$root/${split[1]}"
    } else {
        uri.toString()
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
            val playlists by vm.playlists.collectAsState()
            var showPlaylistDialog by remember { mutableStateOf(false) }
            LazyColumn(Modifier.padding(padding).padding(16.dp)) {
                item {
                    FilledTonalButton(
                        onClick = { showPlaylistDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.QueueMusic, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Adicionar à playlist")
                    }
                    Spacer(Modifier.height(12.dp))
                }
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
            if (showPlaylistDialog) {
                AlertDialog(
                    onDismissRequest = { showPlaylistDialog = false },
                    title = { Text("Adicionar à playlist") },
                    text = {
                        if (playlists.isEmpty()) {
                            Text("Nenhuma playlist criada ainda.")
                        } else {
                            Column {
                                playlists.forEach { playlist ->
                                    ListItem(
                                        headlineContent = { Text(playlist.name) },
                                        leadingContent = { Icon(Icons.Filled.QueueMusic, null) },
                                        modifier = Modifier.clickable {
                                            vm.addToPlaylist(playlist.id, song.id)
                                            showPlaylistDialog = false
                                        }
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { showPlaylistDialog = false }) { Text("Fechar") } }
                )
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
                    SongListItem(song = song, onClick = { onSongClick(song, list) }, onMore = { vm.removeFromPlaylist(playlistId, song.id) })
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
    val playlists by vm.playlists.collectAsState()
    var showPlaylistDialog by remember { mutableStateOf(false) }

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
                        IconButton(onClick = { showPlaylistDialog = true }) {
                            Icon(Icons.Filled.QueueMusic, "Adicionar à playlist")
                        }
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
    if (showPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showPlaylistDialog = false },
            title = { Text("Adicionar pasta à playlist") },
            text = {
                if (playlists.isEmpty()) {
                    Text("Crie uma playlist primeiro.")
                } else {
                    Column {
                        playlists.forEach { playlist ->
                            ListItem(
                                headlineContent = { Text(playlist.name) },
                                supportingContent = { Text("${list.size} músicas serão adicionadas") },
                                leadingContent = { Icon(Icons.Filled.QueueMusic, null) },
                                modifier = Modifier.clickable {
                                    list.forEach { vm.addToPlaylist(playlist.id, it.id) }
                                    showPlaylistDialog = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPlaylistDialog = false }) { Text("Fechar") } }
        )
    }

}
