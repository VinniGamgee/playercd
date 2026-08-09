package com.moonplayer.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("moon_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

class SettingsRepository(private val context: Context) {
    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val GAPLESS = booleanPreferencesKey("gapless")
        val CROSSFADE = booleanPreferencesKey("crossfade")
        val CROSSFADE_MS = intPreferencesKey("crossfade_ms")
        val AUTO_PLAY = booleanPreferencesKey("auto_play")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[Keys.THEME]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    val gapless: Flow<Boolean> = context.dataStore.data.map { it[Keys.GAPLESS] ?: true }
    val crossfade: Flow<Boolean> = context.dataStore.data.map { it[Keys.CROSSFADE] ?: false }
    val crossfadeMs: Flow<Int> = context.dataStore.data.map { it[Keys.CROSSFADE_MS] ?: 500 }
    val autoPlay: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_PLAY] ?: true }

    suspend fun setTheme(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun setGapless(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GAPLESS] = enabled }
    }

    suspend fun setCrossfade(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CROSSFADE] = enabled }
    }

    suspend fun setCrossfadeMs(ms: Int) {
        context.dataStore.edit { it[Keys.CROSSFADE_MS] = ms }
    }

    suspend fun setAutoPlay(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_PLAY] = enabled }
    }
}
