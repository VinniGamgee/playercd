package com.moonplayer.app.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.moonplayer.app.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.*

class PlayerManager(private val context: Context) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private var songMap = mapOf<String, Song>()
    private var transitionFadeEnabled = false
    private var transitionFadeMs = 700
    private val fadeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun configureTransitionFade(enabled: Boolean, durationMs: Int) {
        transitionFadeEnabled = enabled
        transitionFadeMs = durationMs.coerceIn(200, 3000)
    }

    fun connect() {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, token).buildAsync()
        controllerFuture?.addListener({
            controller = controllerFuture?.get()
            controller?.addListener(playerListener)
        }, MoreExecutors.directExecutor())
    }

    fun disconnect() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.mediaId?.let { id ->
                _currentSong.value = songMap[id]
            }
            _duration.value = controller?.duration?.coerceAtLeast(0) ?: 0
            if (transitionFadeEnabled && controller != null) {
                fadeScope.launch {
                    val c = controller ?: return@launch
                    c.volume = 0f
                    val steps = 12
                    val delayMs = (transitionFadeMs / steps).coerceAtLeast(10).toLong()
                    repeat(steps) { step ->
                        c.volume = (step + 1) / steps.toFloat()
                        delay(delayMs)
                    }
                    c.volume = 1f
                }
            }
        }
        override fun onPlaybackStateChanged(playbackState: Int) {
            _duration.value = controller?.duration?.coerceAtLeast(0) ?: 0
        }
        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = repeatMode
        }
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _shuffle.value = shuffleModeEnabled
        }
    }

    fun playSongs(songs: List<Song>, startIndex: Int = 0, autoPlay: Boolean = true, shuffle: Boolean = false) {
        if (songs.isEmpty()) return
        songMap = songs.associateBy { it.id.toString() }
        _queue.value = songs
        val items = songs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(song.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .build()
                )
                .build()
        }
        controller?.setMediaItems(items, startIndex, 0)
        controller?.shuffleModeEnabled = shuffle
        controller?.prepare()
        if (autoPlay) controller?.play()
        _currentSong.value = songs.getOrNull(startIndex)
    }

    fun play() = controller?.play()
    fun pause() = controller?.pause()
    fun togglePlayPause() {
        if (controller?.isPlaying == true) pause() else play()
    }
    fun seekTo(pos: Long) = controller?.seekTo(pos)
    fun skipNext() = controller?.seekToNextMediaItem()
    fun skipPrevious() = controller?.seekToPreviousMediaItem()
    fun setShuffle(enabled: Boolean) {
        controller?.shuffleModeEnabled = enabled
    }
    fun cycleRepeat() {
        val next = when (controller?.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        controller?.repeatMode = next
    }
    fun updatePosition() {
        _position.value = controller?.currentPosition ?: 0
        _duration.value = controller?.duration?.coerceAtLeast(0) ?: 0
    }
    fun removeFromQueue(index: Int) {
        controller?.removeMediaItem(index)
        _queue.value = _queue.value.toMutableList().also { if (index in it.indices) it.removeAt(index) }
    }
    fun clearQueue() {
        controller?.clearMediaItems()
        _queue.value = emptyList()
        _currentSong.value = null
    }

    fun release() {
        fadeScope.cancel()
    }
}
