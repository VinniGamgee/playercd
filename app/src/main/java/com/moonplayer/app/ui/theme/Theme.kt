package com.moonplayer.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.moonplayer.app.data.preferences.AccentPreset
import com.moonplayer.app.data.preferences.UiDensity

private fun accent(preset: AccentPreset, dark: Boolean): Color = when (preset) {
    AccentPreset.MOON -> if (dark) Color(0xFF4FC3F7) else Color(0xFF0277BD)
    AccentPreset.PURPLE -> if (dark) Color(0xFFBB86FC) else Color(0xFF6A1B9A)
    AccentPreset.GREEN -> if (dark) Color(0xFF66BB6A) else Color(0xFF2E7D32)
    AccentPreset.ORANGE -> if (dark) Color(0xFFFFB74D) else Color(0xFFE65100)
    AccentPreset.PINK -> if (dark) Color(0xFFF06292) else Color(0xFFC2185B)
    AccentPreset.RED -> if (dark) Color(0xFFEF5350) else Color(0xFFC62828)
}

private val BgDark = Color(0xFF0A0A0C)
private val SurfaceDark = Color(0xFF141418)
private val SurfaceVariantDark = Color(0xFF1E1E24)
private val OnDark = Color(0xFFF0F0F5)
private val OnDarkMuted = Color(0xFF9A9AA8)

@Composable
fun MoonPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentPreset: AccentPreset = AccentPreset.MOON,
    density: UiDensity = UiDensity.COMFORTABLE,
    cornerRadius: Int = 14,
    amoled: Boolean = false,
    content: @Composable () -> Unit
) {
    val accent = accent(accentPreset, darkTheme)
    val darkScheme = darkColorScheme(
        primary = accent,
        onPrimary = Color.Black,
        primaryContainer = accent.copy(alpha = 0.30f),
        secondary = accent.copy(alpha = 0.75f),
        tertiary = Color(0xFF81C784),
        background = if (amoled) Color.Black else BgDark,
        surface = if (amoled) Color.Black else SurfaceDark,
        surfaceVariant = if (amoled) Color(0xFF111111) else SurfaceVariantDark,
        onBackground = OnDark,
        onSurface = OnDark,
        onSurfaceVariant = OnDarkMuted,
        outline = Color(0xFF3A3A45)
    )
    val lightScheme = lightColorScheme(
        primary = accent,
        onPrimary = Color.White,
        primaryContainer = accent.copy(alpha = 0.20f),
        secondary = accent.copy(alpha = 0.80f),
        tertiary = Color(0xFF388E3C),
        background = Color(0xFFF5F5F7),
        surface = Color.White,
        surfaceVariant = Color(0xFFEEEEF2),
        onBackground = Color(0xFF121216),
        onSurface = Color(0xFF121216),
        onSurfaceVariant = Color(0xFF5C5C6A),
        outline = Color(0xFFC8C8D0)
    )
    val scheme = if (darkTheme) darkScheme else lightScheme
    val base = if (density == UiDensity.COMPACT) 1f else 1.08f
    val typography = Typography(
        headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = (28 * base).sp),
        headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = (22 * base).sp),
        titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = (18 * base).sp),
        titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = (16 * base).sp),
        bodyLarge = TextStyle(fontSize = (16 * base).sp),
        bodyMedium = TextStyle(fontSize = (14 * base).sp),
        labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = (14 * base).sp),
        labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = (11 * base).sp)
    )
    val shapes = Shapes(
        extraSmall = androidx.compose.foundation.shape.RoundedCornerShape((cornerRadius / 2).coerceAtLeast(2).dp),
        small = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius.dp),
        medium = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius.dp),
        large = androidx.compose.foundation.shape.RoundedCornerShape((cornerRadius + 4).dp),
        extraLarge = androidx.compose.foundation.shape.RoundedCornerShape((cornerRadius + 10).dp)
    )
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = scheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = scheme, typography = typography, shapes = shapes, content = content)
}
