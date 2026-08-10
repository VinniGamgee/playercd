package com.moonplayer.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.moonplayer.app.ui.components.AlbumArt
import com.moonplayer.app.ui.components.formatDuration
import com.moonplayer.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onQueue: () -> Unit,
    onSongInfo: (Long) -> Unit,
    onLyrics: () -> Unit
) {
    val song by vm.player.currentSong.collectAsState()
    val isPlaying by vm.player.isPlaying.collectAsState()
    val position by vm.player.position.collectAsState()
    val duration by vm.player.duration.collectAsState()
    val shuffle by vm.player.shuffle.collectAsState()
    val repeatMode by vm.player.repeatMode.collectAsState()

    if (song == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.MusicOff,
                    null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Text("Nenhuma música tocando")
                TextButton(onClick = onBack) { Text("Voltar") }
            }
        }
        return
    }
    val s = song!!

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Tocando agora", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.KeyboardArrowDown, "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = onLyrics) {
                        Icon(Icons.Filled.Lyrics, "Letras")
                    }
                    IconButton(onClick = onQueue) {
                        Icon(Icons.Filled.QueueMusic, "Fila")
                    }
                    IconButton(onClick = { onSongInfo(s.id) }) {
                        Icon(Icons.Filled.Info, "Info")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(8.dp))
                AlbumArt(song = s, size = 300.dp, corner = 16.dp)
                Spacer(Modifier.height(28.dp))

                Text(
                    s.title,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    s.artist,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    s.album,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(20.dp))

                val progress = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
                Slider(
                    value = progress,
                    onValueChange = { vm.player.seekTo((it * duration).toLong()) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        formatDuration(position),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatDuration(duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Secondary actions
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = { vm.toggleFavorite(s) }) {
                        Icon(
                            if (s.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            "Favorito",
                            tint = if (s.isFavorite) Color(0xFFE91E63)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onLyrics) {
                        Icon(
                            Icons.Filled.Lyrics,
                            "Letras",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onQueue) {
                        Icon(
                            Icons.Filled.PlaylistPlay,
                            "Fila",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onSongInfo(s.id) }) {
                        Icon(
                            Icons.Filled.MoreHoriz,
                            "Mais",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Main transport
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { vm.player.setShuffle(!shuffle) }) {
                        Icon(
                            Icons.Filled.Shuffle,
                            "Shuffle",
                            tint = if (shuffle) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { vm.player.skipPrevious() }) {
                        Icon(
                            Icons.Filled.SkipPrevious,
                            "Anterior",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    FilledIconButton(
                        onClick = { vm.player.togglePlayPause() },
                        modifier = Modifier.size(72.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            null,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    IconButton(onClick = { vm.player.skipNext() }) {
                        Icon(
                            Icons.Filled.SkipNext,
                            "Próxima",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    IconButton(onClick = { vm.player.cycleRepeat() }) {
                        Icon(
                            when (repeatMode) {
                                Player.REPEAT_MODE_ONE -> Icons.Filled.RepeatOne
                                else -> Icons.Filled.Repeat
                            },
                            "Repetir",
                            tint = if (repeatMode != Player.REPEAT_MODE_OFF)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                AssistChip(
                    onClick = onLyrics,
                    label = { Text("Ver letras") },
                    leadingIcon = { Icon(Icons.Filled.Lyrics, null, Modifier.size(18.dp)) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val lyrics by vm.lyrics.collectAsState()
    val timedLyrics by vm.timedLyrics.collectAsState()
    val position by vm.player.position.collectAsState()
    val song by vm.player.currentSong.collectAsState()
    val listState = rememberLazyListState()

    val activeIndex = remember(position, timedLyrics) {
        timedLyrics.indexOfLast { it.timeMs <= position }
    }

    LaunchedEffect(activeIndex, timedLyrics.size) {
        if (activeIndex >= 0 && activeIndex < timedLyrics.size) {
            listState.animateScrollToItem(activeIndex.coerceAtLeast(0))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(song?.title ?: "Letras", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        if (lyrics.isNullOrBlank()) {
            Box(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Lyrics,
                        null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Sem letras embutidas", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Esta faixa não possui letras no arquivo.\nLetras online ficam para uma próxima etapa.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (timedLyrics.isNotEmpty()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 120.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                itemsIndexed(timedLyrics) { index, line ->
                    Text(
                        text = line.text,
                        style = if (index == activeIndex) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                        color = if (index == activeIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (index == activeIndex) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 32.dp)
            ) {
                item {
                    Text(
                        lyrics!!,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
