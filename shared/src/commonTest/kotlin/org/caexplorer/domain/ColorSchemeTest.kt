package org.caexplorer.domain

import org.caexplorer.domain.colorscheme.*
import kotlin.test.*

class ColorSchemeTest {

    @Test
    fun rainbowEmptyAndFilledColors() {
        val scheme = RainbowColorScheme()
        val empty = scheme.getColor(0, 10)
        val filled = scheme.getColor(9, 10)
        assertEquals(scheme.emptyColor, empty)
        assertEquals(scheme.filledColor, filled)
    }

    @Test
    fun gradientInterpolation() {
        val scheme = BlackAndWhiteColorScheme()
        // state 0 = white (empty), state 1 = black (filled) for 2 states
        val white = scheme.getColor(0, 2)
        val black = scheme.getColor(1, 2)
        assertEquals(1f, white.red, 0.01f)
        assertEquals(0f, black.red, 0.01f)
    }

    @Test
    fun allColorSchemesExist() {
        val schemes = ALL_COLOR_SCHEMES
        assertTrue(schemes.size >= 10, "Expected 10+ color schemes, got ${schemes.size}")
        val names = schemes.map { it.displayName }
        assertTrue("Rainbow" in names)
        assertTrue("Fire" in names)
        assertTrue("Black and White" in names)
    }

    @Test
    fun colorSchemeDisplayNames() {
        ALL_COLOR_SCHEMES.forEach { scheme ->
            assertTrue(scheme.displayName.isNotBlank(), "Scheme has blank name")
        }
    }

    @Test
    fun interpolateFunction() {
        val from = androidx.compose.ui.graphics.Color.Black
        val to = androidx.compose.ui.graphics.Color.White
        val mid = ColorScheme.interpolate(from, to, 0.5f)
        assertEquals(0.5f, mid.red, 0.01f)
        assertEquals(0.5f, mid.green, 0.01f)
        assertEquals(0.5f, mid.blue, 0.01f)
    }
}
