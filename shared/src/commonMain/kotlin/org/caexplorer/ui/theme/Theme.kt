package org.caexplorer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Custom dark-only Material 3 color scheme.
 * Warm orange/fire palette from Material Theme Builder.
 */
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB59B),
    onPrimary = Color(0xFF5C1A00),
    primaryContainer = Color(0xFFCF4500),
    onPrimaryContainer = Color(0xFFFFFBFF),
    secondary = Color(0xFFFFB59C),
    onSecondary = Color(0xFF5C1900),
    secondaryContainer = Color(0xFFFF570B),
    onSecondaryContainer = Color(0xFF511500),
    tertiary = Color(0xFFCECC47),
    onTertiary = Color(0xFF333200),
    tertiaryContainer = Color(0xFF979509),
    onTertiaryContainer = Color(0xFF2C2B00),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFFFF5449),
    onErrorContainer = Color(0xFF5C0004),
    background = Color(0xFF1D100C),
    onBackground = Color(0xFFF8DDD4),
    surface = Color(0xFF1D100C),
    onSurface = Color(0xFFF8DDD4),
    surfaceVariant = Color(0xFF5A4139),
    onSurfaceVariant = Color(0xFFE2BFB3),
    outline = Color(0xFFA98A7F),
    outlineVariant = Color(0xFF5A4139),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFF8DDD4),
    inverseOnSurface = Color(0xFF3D2D27),
    inversePrimary = Color(0xFFAA3700),
    surfaceDim = Color(0xFF1D100C),
    surfaceBright = Color(0xFF463530),
    surfaceContainerLowest = Color(0xFF170B07),
    surfaceContainerLow = Color(0xFF261813),
    surfaceContainer = Color(0xFF2B1C17),
    surfaceContainerHigh = Color(0xFF362621),
    surfaceContainerHighest = Color(0xFF41312B),
)

@Composable
fun CAExplorerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = CATypography,
        content = content
    )
}
