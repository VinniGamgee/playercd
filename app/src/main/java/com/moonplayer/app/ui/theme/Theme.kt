package com.moonplayer.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val MoonBlue = Color(0xFF5B8DEF)
private val MoonPurple = Color(0xFF8B7EC8)
private val MoonDark = Color(0xFF0D1117)
private val MoonSurface = Color(0xFF161B22)
private val MoonSurfaceLight = Color(0xFFF6F8FA)

private val DarkColorScheme = darkColorScheme(
    primary = MoonBlue,
    secondary = MoonPurple,
    tertiary = Color(0xFF7EE787),
    background = MoonDark,
    surface = MoonSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFE6EDF3),
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = Color(0xFF21262D),
    onSurfaceVariant = Color(0xFF8B949E)
)

private val LightColorScheme = lightColorScheme(
    primary = MoonBlue,
    secondary = MoonPurple,
    tertiary = Color(0xFF1A7F37),
    background = Color.White,
    surface = MoonSurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1F2328),
    onSurface = Color(0xFF1F2328),
    surfaceVariant = Color(0xFFEAEEF2),
    onSurfaceVariant = Color(0xFF656D76)
)

@Composable
fun MoonPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

val Typography = Typography()
