package com.moonplayer.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moonplayer.app.ui.components.AlbumArt
import com.moonplayer.app.ui.components.formatDuration
import com.moonplayer.app.viewmodel.MainViewModel
import androidx.media3.common.Player

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onQueue: () -> Unit,
    onSongInfo: (Long) -> Unit
) {
    val song by vm.player.currentSong.collectAsState()
    val isPlaying by vm.player.isPlaying.collectAsState()
    val position by vm.player.position.collectAsState()
    val duration by vm.player.duration.collectAsState()
    val shuffle by vm.player.shuffle.collectAsState()
    val repeatMode by vm.player.repeatMode.collectAsState()

    if (song == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nenhuma música tocando")
        }
        return
    }
    val s = song!!

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tocando agora") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.KeyboardArrowDown, "Voltar") }
                },
                actions = {
                    IconButton(onClick = onQueue) { Icon(Icons.Filled.QueueMusic, "Fila") }
                    IconButton(onClick = { onSongInfo(s.id) }) { Icon(Icons.Filled.Info, "Info") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            AlbumArt(song = s, size = 280.dp)
            Spacer(Modifier.height(32.dp))
            Text(
                s.title,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(s.artist, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(s.album, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))

            val progress = if (duration > 0) position.toFloat() / duration else 0f
            Slider(
                value = progress.coerceIn(0f, 1f),
                onValueChange = { vm.player.seekTo((it * duration).toLong()) },
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDuration(position), style = MaterialTheme.typography.labelSmall)
                Text(formatDuration(duration), style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(16.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.player.setShuffle(!shuffle) }) {
                    Icon(
                        Icons.Filled.Shuffle, "Shuffle",
                        tint = if (shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { vm.player.skipPrevious() }) {
                    Icon(Icons.Filled.SkipPrevious, "Anterior", modifier = Modifier.size(36.dp))
                }
                FilledIconButton(
                    onClick = { vm.player.togglePlayPause() },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        null,
                        modifier = Modifier.size(36.dp)
                    )
                }
                IconButton(onClick = { vm.player.skipNext() }) {
                    Icon(Icons.Filled.SkipNext, "Próxima", modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = { vm.player.cycleRepeat() }) {
                    Icon(
                        when (repeatMode) {
                            Player.REPEAT_MODE_ONE -> Icons.Filled.RepeatOne
                            Player.REPEAT_MODE_ALL -> Icons.Filled.Repeat
                            else -> Icons.Filled.Repeat
                        },
                        "Repetir",
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            IconButton(onClick = { vm.toggleFavorite(s) }) {
                Icon(
                    if (s.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    "Favorito",
                    tint = if (s.isFavorite) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
