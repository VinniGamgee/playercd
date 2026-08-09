package com.moonplayer.app.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.moonplayer.app.ui.components.MiniPlayer
import com.moonplayer.app.ui.screens.*
import com.moonplayer.app.viewmodel.MainViewModel
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    data object Library : Screen("library")
    data object Player : Screen("player")
    data object Queue : Screen("queue")
    data object Settings : Screen("settings")
    data object Search : Screen("search")
    data object Lyrics : Screen("lyrics")
    data object SongInfo : Screen("song_info/{songId}") {
        fun create(id: Long) = "song_info/$id"
    }
    data object ArtistDetail : Screen("artist/{name}") {
        fun create(name: String) =
            "artist/${URLEncoder.encode(name, StandardCharsets.UTF_8.toString())}"
    }
    data object AlbumDetail : Screen("album/{id}") {
        fun create(id: Long) = "album/$id"
    }
    data object PlaylistDetail : Screen("playlist/{id}") {
        fun create(id: Long) = "playlist/$id"
    }
    data object FolderDetail : Screen("folder/{path}") {
        fun create(path: String) =
            "folder/${URLEncoder.encode(path, StandardCharsets.UTF_8.toString())}"
    }
}

@Composable
fun MoonNavHost(vm: MainViewModel) {
    val navController = rememberNavController()
    val currentSong by vm.player.currentSong.collectAsState()
    val isPlaying by vm.player.isPlaying.collectAsState()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showMini = currentSong != null &&
        currentRoute != Screen.Player.route &&
        currentRoute != Screen.Lyrics.route

    Scaffold(
        bottomBar = {
            Column {
                if (showMini) {
                    MiniPlayer(
                        song = currentSong!!,
                        isPlaying = isPlaying,
                        onPlayPause = { vm.player.togglePlayPause() },
                        onClick = { navController.navigate(Screen.Player.route) },
                        onNext = { vm.player.skipNext() }
                    )
                }
                if (currentRoute in listOf(
                        Screen.Library.route,
                        Screen.Settings.route,
                        Screen.Search.route
                    )
                ) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentRoute == Screen.Library.route,
                            onClick = {
                                navController.navigate(Screen.Library.route) {
                                    popUpTo(Screen.Library.route) { inclusive = true }
                                }
                            },
                            icon = {
                                Icon(
                                    if (currentRoute == Screen.Library.route)
                                        Icons.Filled.LibraryMusic
                                    else Icons.Outlined.LibraryMusic,
                                    null
                                )
                            },
                            label = { Text("Biblioteca") }
                        )
                        NavigationBarItem(
                            selected = currentRoute == Screen.Search.route,
                            onClick = { navController.navigate(Screen.Search.route) },
                            icon = {
                                Icon(
                                    if (currentRoute == Screen.Search.route)
                                        Icons.Filled.Search
                                    else Icons.Outlined.Search,
                                    null
                                )
                            },
                            label = { Text("Buscar") }
                        )
                        NavigationBarItem(
                            selected = currentRoute == Screen.Settings.route,
                            onClick = { navController.navigate(Screen.Settings.route) },
                            icon = {
                                Icon(
                                    if (currentRoute == Screen.Settings.route)
                                        Icons.Filled.Settings
                                    else Icons.Outlined.Settings,
                                    null
                                )
                            },
                            label = { Text("Ajustes") }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Library.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Library.route) {
                LibraryScreen(
                    vm = vm,
                    onSongClick = { song, list -> vm.playSong(song, list) },
                    onArtistClick = { navController.navigate(Screen.ArtistDetail.create(it)) },
                    onAlbumClick = { navController.navigate(Screen.AlbumDetail.create(it)) },
                    onPlaylistClick = { navController.navigate(Screen.PlaylistDetail.create(it)) },
                    onFolderClick = { navController.navigate(Screen.FolderDetail.create(it)) },
                    onSongInfo = { navController.navigate(Screen.SongInfo.create(it)) }
                )
            }
            composable(Screen.Player.route) {
                PlayerScreen(
                    vm = vm,
                    onBack = { navController.popBackStack() },
                    onQueue = { navController.navigate(Screen.Queue.route) },
                    onSongInfo = { id -> navController.navigate(Screen.SongInfo.create(id)) },
                    onLyrics = {
                        currentSong?.let { vm.loadLyricsPlaceholder(it) }
                        navController.navigate(Screen.Lyrics.route)
                    }
                )
            }
            composable(Screen.Lyrics.route) {
                LyricsScreen(vm = vm, onBack = { navController.popBackStack() })
            }
            composable(Screen.Queue.route) {
                QueueScreen(vm = vm, onBack = { navController.popBackStack() })
            }
            composable(Screen.Settings.route) {
                SettingsScreen(vm = vm)
            }
            composable(Screen.Search.route) {
                SearchScreen(
                    vm = vm,
                    onSongClick = { song, list -> vm.playSong(song, list) }
                )
            }
            composable(
                Screen.SongInfo.route,
                arguments = listOf(navArgument("songId") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("songId") ?: return@composable
                SongInfoScreen(vm = vm, songId = id, onBack = { navController.popBackStack() })
            }
            composable(
                Screen.ArtistDetail.route,
                arguments = listOf(navArgument("name") { type = NavType.StringType })
            ) { entry ->
                val raw = entry.arguments?.getString("name") ?: return@composable
                val name = URLDecoder.decode(raw, StandardCharsets.UTF_8.toString())
                ArtistDetailScreen(
                    vm = vm,
                    artist = name,
                    onBack = { navController.popBackStack() },
                    onSongClick = { s, l -> vm.playSong(s, l) }
                )
            }
            composable(
                Screen.AlbumDetail.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: return@composable
                AlbumDetailScreen(
                    vm = vm,
                    albumId = id,
                    onBack = { navController.popBackStack() },
                    onSongClick = { s, l -> vm.playSong(s, l) }
                )
            }
            composable(
                Screen.PlaylistDetail.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: return@composable
                PlaylistDetailScreen(
                    vm = vm,
                    playlistId = id,
                    onBack = { navController.popBackStack() },
                    onSongClick = { s, l -> vm.playSong(s, l) }
                )
            }
            composable(
                Screen.FolderDetail.route,
                arguments = listOf(navArgument("path") { type = NavType.StringType })
            ) { entry ->
                val raw = entry.arguments?.getString("path") ?: return@composable
                val path = URLDecoder.decode(raw, StandardCharsets.UTF_8.toString())
                FolderDetailScreen(
                    vm = vm,
                    folderPath = path,
                    onBack = { navController.popBackStack() },
                    onSongClick = { s, l -> vm.playSong(s, l) }
                )
            }
        }
    }
}
