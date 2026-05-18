package org.caexplorer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Dynamic Material 3 theming with user-selected seed color.
 * Generates a full M3 palette from a single seed color.
 */
object ThemeState {
    var seedColor by mutableStateOf(Color(0xFF1B6EF3)) // Default: vibrant blue
    var useDarkTheme by mutableStateOf<Boolean?>(null) // null = follow system
}

/**
 * Attempt to generate tonal palette from a seed color.
 * Uses a simplified HCT-inspired algorithm for cross-platform support.
 */
fun generateColorScheme(seedColor: Color, isDark: Boolean): ColorScheme {
    val hsl = colorToHsl(seedColor)
    val hue = hsl[0]

    return if (isDark) {
        darkColorScheme(
            primary = hslToColor(hue, 0.80f, 0.70f),
            onPrimary = hslToColor(hue, 0.30f, 0.15f),
            primaryContainer = hslToColor(hue, 0.60f, 0.25f),
            onPrimaryContainer = hslToColor(hue, 0.80f, 0.85f),
            secondary = hslToColor(hue + 30f, 0.40f, 0.70f),
            onSecondary = hslToColor(hue + 30f, 0.20f, 0.15f),
            secondaryContainer = hslToColor(hue + 30f, 0.30f, 0.25f),
            onSecondaryContainer = hslToColor(hue + 30f, 0.40f, 0.85f),
            tertiary = hslToColor(hue + 60f, 0.50f, 0.70f),
            onTertiary = hslToColor(hue + 60f, 0.25f, 0.15f),
            tertiaryContainer = hslToColor(hue + 60f, 0.35f, 0.25f),
            onTertiaryContainer = hslToColor(hue + 60f, 0.50f, 0.85f),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            background = Color(0xFF1A1C1E),
            onBackground = Color(0xFFE2E2E6),
            surface = Color(0xFF1A1C1E),
            onSurface = Color(0xFFE2E2E6),
            surfaceVariant = hslToColor(hue, 0.10f, 0.28f),
            onSurfaceVariant = Color(0xFFC3C6CF),
            outline = Color(0xFF8D9199),
            outlineVariant = Color(0xFF43474E),
            inverseSurface = Color(0xFFE2E2E6),
            inverseOnSurface = Color(0xFF2F3033),
            inversePrimary = hslToColor(hue, 0.70f, 0.40f),
            surfaceTint = hslToColor(hue, 0.80f, 0.70f),
        )
    } else {
        lightColorScheme(
            primary = hslToColor(hue, 0.70f, 0.40f),
            onPrimary = Color.White,
            primaryContainer = hslToColor(hue, 0.80f, 0.88f),
            onPrimaryContainer = hslToColor(hue, 0.70f, 0.15f),
            secondary = hslToColor(hue + 30f, 0.30f, 0.40f),
            onSecondary = Color.White,
            secondaryContainer = hslToColor(hue + 30f, 0.40f, 0.88f),
            onSecondaryContainer = hslToColor(hue + 30f, 0.30f, 0.12f),
            tertiary = hslToColor(hue + 60f, 0.40f, 0.38f),
            onTertiary = Color.White,
            tertiaryContainer = hslToColor(hue + 60f, 0.50f, 0.88f),
            onTertiaryContainer = hslToColor(hue + 60f, 0.40f, 0.12f),
            error = Color(0xFFBA1A1A),
            onError = Color.White,
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            background = Color(0xFFFCFCFF),
            onBackground = Color(0xFF1A1C1E),
            surface = Color(0xFFFCFCFF),
            onSurface = Color(0xFF1A1C1E),
            surfaceVariant = hslToColor(hue, 0.12f, 0.90f),
            onSurfaceVariant = Color(0xFF43474E),
            outline = Color(0xFF73777F),
            outlineVariant = Color(0xFFC3C6CF),
            inverseSurface = Color(0xFF2F3033),
            inverseOnSurface = Color(0xFFF1F0F4),
            inversePrimary = hslToColor(hue, 0.80f, 0.70f),
            surfaceTint = hslToColor(hue, 0.70f, 0.40f),
        )
    }
}

@Composable
fun CAExplorerTheme(
    seedColor: Color = ThemeState.seedColor,
    darkTheme: Boolean = ThemeState.useDarkTheme ?: isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = generateColorScheme(seedColor, darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CATypography,
        content = content
    )
}

// --- HSL utilities ---

private fun colorToHsl(color: Color): FloatArray {
    val r = color.red
    val g = color.green
    val b = color.blue

    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2f

    if (max == min) return floatArrayOf(0f, 0f, l)

    val d = max - min
    val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
    val h = when (max) {
        r -> ((g - b) / d + (if (g < b) 6f else 0f)) * 60f
        g -> ((b - r) / d + 2f) * 60f
        else -> ((r - g) / d + 4f) * 60f
    }

    return floatArrayOf(h, s, l)
}

private fun hslToColor(h: Float, s: Float, l: Float): Color {
    val hue = ((h % 360f) + 360f) % 360f
    val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
    val x = c * (1f - kotlin.math.abs((hue / 60f) % 2f - 1f))
    val m = l - c / 2f

    val (r, g, b) = when {
        hue < 60f -> Triple(c, x, 0f)
        hue < 120f -> Triple(x, c, 0f)
        hue < 180f -> Triple(0f, c, x)
        hue < 240f -> Triple(0f, x, c)
        hue < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    return Color(r + m, g + m, b + m)
}
