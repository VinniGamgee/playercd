package com.moonplayer.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("moon_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED }
enum class AccentPreset { MOON, PURPLE, GREEN, ORANGE, PINK, RED }
enum class UiDensity { COMFORTABLE, COMPACT }
enum class LibrarySort { TITLE, ARTIST, ALBUM, DATE_ADDED, DURATION }

data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentPreset = AccentPreset.MOON,
    val density: UiDensity = UiDensity.COMFORTABLE,
    val cornerRadius: Int = 14,
    val librarySort: LibrarySort = LibrarySort.TITLE,
    val scanSubfolders: Boolean = true,
    val autoScan: Boolean = true,
    val showRecentlyPlayed: Boolean = true,
    val showFavorites: Boolean = true,
    val showPlaylists: Boolean = true,
    val showArtists: Boolean = true,
    val showAlbums: Boolean = true,
    val showFolders: Boolean = true,
    val gapless: Boolean = true,
    val crossfade: Boolean = false,
    val crossfadeMs: Int = 700,
    val autoPlay: Boolean = true,
    val resumePlayback: Boolean = true,
    val pauseOnNoisy: Boolean = true,
    val showCodecInfo: Boolean = true,
    val showAlbumArt: Boolean = true
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val ACCENT = stringPreferencesKey("accent")
        val DENSITY = stringPreferencesKey("density")
        val CORNER_RADIUS = intPreferencesKey("corner_radius")
        val LIBRARY_SORT = stringPreferencesKey("library_sort")
        val SCAN_SUBFOLDERS = booleanPreferencesKey("scan_subfolders")
        val AUTO_SCAN = booleanPreferencesKey("auto_scan")
        val SHOW_RECENT = booleanPreferencesKey("show_recent")
        val SHOW_FAVORITES = booleanPreferencesKey("show_favorites")
        val SHOW_PLAYLISTS = booleanPreferencesKey("show_playlists")
        val SHOW_ARTISTS = booleanPreferencesKey("show_artists")
        val SHOW_ALBUMS = booleanPreferencesKey("show_albums")
        val SHOW_FOLDERS = booleanPreferencesKey("show_folders")
        val GAPLESS = booleanPreferencesKey("gapless")
        val CROSSFADE = booleanPreferencesKey("crossfade")
        val CROSSFADE_MS = intPreferencesKey("crossfade_ms")
        val AUTO_PLAY = booleanPreferencesKey("auto_play")
        val RESUME = booleanPreferencesKey("resume")
        val PAUSE_NOISY = booleanPreferencesKey("pause_noisy")
        val SHOW_CODEC = booleanPreferencesKey("show_codec")
        val SHOW_ART = booleanPreferencesKey("show_art")
        val INCLUDE_PATHS = stringSetPreferencesKey("include_paths")
        val EXCLUDE_PATHS = stringSetPreferencesKey("exclude_paths")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            theme = runCatching { ThemeMode.valueOf(p[Keys.THEME] ?: ThemeMode.SYSTEM.name) }.getOrDefault(ThemeMode.SYSTEM),
            accent = runCatching { AccentPreset.valueOf(p[Keys.ACCENT] ?: AccentPreset.MOON.name) }.getOrDefault(AccentPreset.MOON),
            density = runCatching { UiDensity.valueOf(p[Keys.DENSITY] ?: UiDensity.COMFORTABLE.name) }.getOrDefault(UiDensity.COMFORTABLE),
            cornerRadius = (p[Keys.CORNER_RADIUS] ?: 14).coerceIn(4, 28),
            librarySort = runCatching { LibrarySort.valueOf(p[Keys.LIBRARY_SORT] ?: LibrarySort.TITLE.name) }.getOrDefault(LibrarySort.TITLE),
            scanSubfolders = p[Keys.SCAN_SUBFOLDERS] ?: true,
            autoScan = p[Keys.AUTO_SCAN] ?: true,
            showRecentlyPlayed = p[Keys.SHOW_RECENT] ?: true,
            showFavorites = p[Keys.SHOW_FAVORITES] ?: true,
            showPlaylists = p[Keys.SHOW_PLAYLISTS] ?: true,
            showArtists = p[Keys.SHOW_ARTISTS] ?: true,
            showAlbums = p[Keys.SHOW_ALBUMS] ?: true,
            showFolders = p[Keys.SHOW_FOLDERS] ?: true,
            gapless = p[Keys.GAPLESS] ?: true,
            crossfade = p[Keys.CROSSFADE] ?: false,
            crossfadeMs = (p[Keys.CROSSFADE_MS] ?: 700).coerceIn(200, 3000),
            autoPlay = p[Keys.AUTO_PLAY] ?: true,
            resumePlayback = p[Keys.RESUME] ?: true,
            pauseOnNoisy = p[Keys.PAUSE_NOISY] ?: true,
            showCodecInfo = p[Keys.SHOW_CODEC] ?: true,
            showAlbumArt = p[Keys.SHOW_ART] ?: true
        )
    }

    val includePaths: Flow<Set<String>> = context.dataStore.data.map { it[Keys.INCLUDE_PATHS] ?: emptySet() }
    val excludePaths: Flow<Set<String>> = context.dataStore.data.map { it[Keys.EXCLUDE_PATHS] ?: emptySet() }

    suspend fun setTheme(v: ThemeMode) = edit { it[Keys.THEME] = v.name }
    suspend fun setAccent(v: AccentPreset) = edit { it[Keys.ACCENT] = v.name }
    suspend fun setDensity(v: UiDensity) = edit { it[Keys.DENSITY] = v.name }
    suspend fun setCornerRadius(v: Int) = edit { it[Keys.CORNER_RADIUS] = v.coerceIn(4, 28) }
    suspend fun setLibrarySort(v: LibrarySort) = edit { it[Keys.LIBRARY_SORT] = v.name }
    suspend fun setScanSubfolders(v: Boolean) = edit { it[Keys.SCAN_SUBFOLDERS] = v }
    suspend fun setAutoScan(v: Boolean) = edit { it[Keys.AUTO_SCAN] = v }
    suspend fun setRecently(v: Boolean) = edit { it[Keys.SHOW_RECENT] = v }
    suspend fun setFavorites(v: Boolean) = edit { it[Keys.SHOW_FAVORITES] = v }
    suspend fun setPlaylists(v: Boolean) = edit { it[Keys.SHOW_PLAYLISTS] = v }
    suspend fun setArtists(v: Boolean) = edit { it[Keys.SHOW_ARTISTS] = v }
    suspend fun setAlbums(v: Boolean) = edit { it[Keys.SHOW_ALBUMS] = v }
    suspend fun setFolders(v: Boolean) = edit { it[Keys.SHOW_FOLDERS] = v }
    suspend fun setGapless(v: Boolean) = edit { it[Keys.GAPLESS] = v }
    suspend fun setCrossfade(v: Boolean) = edit { it[Keys.CROSSFADE] = v }
    suspend fun setCrossfadeMs(v: Int) = edit { it[Keys.CROSSFADE_MS] = v.coerceIn(200, 3000) }
    suspend fun setAutoPlay(v: Boolean) = edit { it[Keys.AUTO_PLAY] = v }
    suspend fun setResume(v: Boolean) = edit { it[Keys.RESUME] = v }
    suspend fun setPauseNoisy(v: Boolean) = edit { it[Keys.PAUSE_NOISY] = v }
    suspend fun setShowCodec(v: Boolean) = edit { it[Keys.SHOW_CODEC] = v }
    suspend fun setShowArt(v: Boolean) = edit { it[Keys.SHOW_ART] = v }

    suspend fun addIncludePath(path: String) = edit { p -> p[Keys.INCLUDE_PATHS] = (p[Keys.INCLUDE_PATHS] ?: emptySet()) + path }
    suspend fun removeIncludePath(path: String) = edit { p -> p[Keys.INCLUDE_PATHS] = (p[Keys.INCLUDE_PATHS] ?: emptySet()) - path }
    suspend fun addExcludePath(path: String) = edit { p -> p[Keys.EXCLUDE_PATHS] = (p[Keys.EXCLUDE_PATHS] ?: emptySet()) + path }
    suspend fun removeExcludePath(path: String) = edit { p -> p[Keys.EXCLUDE_PATHS] = (p[Keys.EXCLUDE_PATHS] ?: emptySet()) - path }
    suspend fun clearLibraryPaths() = edit {
        it.remove(Keys.INCLUDE_PATHS)
        it.remove(Keys.EXCLUDE_PATHS)
    }

    suspend fun reset() { context.dataStore.edit { it.clear() } }

    private suspend fun edit(block: suspend (MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
