package org.caexplorer.domain.colorscheme

import androidx.compose.ui.graphics.Color
import kotlin.math.absoluteValue
import kotlin.random.Random

// --- Gradient-based schemes (use linear interpolation between two colors) ---

class BlackAndWhiteColorScheme : GradientColorScheme(
    emptyColor = Color.White,
    filledColor = Color.Black,
    emptyGradientColor = Color.White,
    filledGradientColor = Color.Black
) {
    override val displayName = "Black and White"
}

class WhiteAndBlackColorScheme : GradientColorScheme(
    emptyColor = Color.Black,
    filledColor = Color.White,
    emptyGradientColor = Color.Black,
    filledGradientColor = Color.White
) {
    override val displayName = "White and Black"
}

class FireColorScheme : GradientColorScheme(
    emptyColor = Color.Black,
    filledColor = Color.Red,
    emptyGradientColor = Color.Yellow,
    filledGradientColor = Color.Red
) {
    override val displayName = "Fire"
}

class BlueDiamondColorScheme : GradientColorScheme(
    emptyColor = Color(20, 20, 45),
    filledColor = Color(0, 0, 255),
    emptyGradientColor = Color(20, 20, 45),
    filledGradientColor = Color(230, 230, 255)
) {
    override val displayName = "Blue Diamond"
}

class ChocolateColorScheme : GradientColorScheme(
    emptyColor = Color(170, 170, 170),
    filledColor = Color(26, 13, 3),
    emptyGradientColor = Color(170, 170, 170),
    filledGradientColor = Color(26, 13, 3)
) {
    override val displayName = "Chocolate"
}

class GreenOceanColorScheme : GradientColorScheme(
    emptyColor = Color.Black,
    filledColor = Color(102, 255, 0),
    emptyGradientColor = Color.Blue,
    filledGradientColor = Color(102, 255, 0)
) {
    override val displayName = "Green Ocean"
}

class YellowJacketColorScheme : GradientColorScheme(
    emptyColor = Color(20, 20, 45),
    filledColor = Color(255, 255, 0),
    emptyGradientColor = Color(20, 20, 45),
    filledGradientColor = Color(230, 230, 255)
) {
    override val displayName = "Yellow Jacket"
}

// --- HSB-based schemes (use hue rotation for distinct colors per state) ---

/**
 * Rainbow color scheme with distinct hue-rotated colors for each state value.
 * Port of Java RainbowColorScheme.
 */
class RainbowColorScheme : GradientColorScheme(
    emptyColor = ColorScheme.DEFAULT_EMPTY_COLOR,
    filledColor = ColorScheme.DEFAULT_FILLED_COLOR
) {
    override val displayName = "Rainbow"
    private var cachedColors: Array<Color>? = null
    private var cachedNumStates = -1

    @Synchronized
    override fun getColor(state: Int, numStates: Int): Color {
        if (numStates <= 1) return emptyColor
        if (state == 0) return emptyColor
        if (state == numStates - 1) return filledColor

        if (cachedColors == null || cachedNumStates != numStates) {
            cachedNumStates = numStates
            cachedColors = generateRainbowPalette(numStates)
        }
        return cachedColors!![state.coerceIn(0, numStates - 1)]
    }

    private fun generateRainbowPalette(numStates: Int): Array<Color> {
        return Array(numStates) { i ->
            when (i) {
                0 -> emptyColor
                numStates - 1 -> filledColor
                else -> {
                    val hue = (i.toFloat() / numStates) * 360f
                    hsbToColor(hue, 1.0f, 1.0f)
                }
            }
        }
    }
}

/**
 * Shades-based scheme using a two-color linear gradient cached per numStates.
 * Used by KindOfBlues, PurpleHaze, WaterLilies.
 */
open class ShadesColorScheme(
    private val shadesEmptyColor: Color,
    private val shadesFilledColor: Color,
    override val displayName: String
) : GradientColorScheme(
    emptyColor = shadesEmptyColor,
    filledColor = shadesFilledColor
) {
    private var cachedColors: Array<Color>? = null
    private var cachedNumStates = -1

    @Synchronized
    override fun getColor(state: Int, numStates: Int): Color {
        if (numStates <= 1) return emptyColor
        if (cachedColors == null || cachedNumStates != numStates) {
            cachedNumStates = numStates
            cachedColors = Array(numStates) { i ->
                val fraction = i.toFloat() / (numStates - 1).toFloat()
                ColorScheme.interpolate(shadesEmptyColor, shadesFilledColor, fraction)
            }
            emptyColor = cachedColors!![0]
            filledColor = cachedColors!![numStates - 1]
        }
        return cachedColors!![state.coerceIn(0, numStates - 1)]
    }
}

class KindOfBluesColorScheme : ShadesColorScheme(
    shadesEmptyColor = Color(191, 191, 255),
    shadesFilledColor = Color(0, 0, 102),
    displayName = "Kind of Blues"
)

class PurpleHazeColorScheme : ShadesColorScheme(
    shadesEmptyColor = Color(75, 0, 75),
    shadesFilledColor = Color(230, 0, 230),
    displayName = "Purple Haze"
)

class WaterLiliesColorScheme : ShadesColorScheme(
    shadesEmptyColor = Color(0, 0, 102),
    shadesFilledColor = Color(102, 255, 0),
    displayName = "Water Lilies"
)

/**
 * Random color scheme — generates a random palette on first access.
 */
class RandomColorScheme : GradientColorScheme(
    emptyColor = Color(
        Random.nextInt(256),
        Random.nextInt(256),
        Random.nextInt(256)
    ),
    filledColor = Color(
        Random.nextInt(256),
        Random.nextInt(256),
        Random.nextInt(256)
    )
) {
    override val displayName = "Random"
    private var randomColors: Array<Color>? = null
    private var cachedNumStates = -1

    @Synchronized
    override fun getColor(state: Int, numStates: Int): Color {
        if (numStates <= 1) return emptyColor
        if (randomColors == null || cachedNumStates != numStates) {
            cachedNumStates = numStates
            randomColors = Array(numStates) { i ->
                when (i) {
                    0 -> emptyColor
                    numStates - 1 -> filledColor
                    else -> Color(
                        Random.nextInt(256),
                        Random.nextInt(256),
                        Random.nextInt(256)
                    )
                }
            }
        }
        return randomColors!![state.coerceIn(0, numStates - 1)]
    }
}

/** All built-in color schemes. */
val ALL_COLOR_SCHEMES: List<ColorScheme> by lazy {
    listOf(
        RainbowColorScheme(),
        KindOfBluesColorScheme(),
        FireColorScheme(),
        GreenOceanColorScheme(),
        BlueDiamondColorScheme(),
        PurpleHazeColorScheme(),
        WaterLiliesColorScheme(),
        YellowJacketColorScheme(),
        BlackAndWhiteColorScheme(),
        WhiteAndBlackColorScheme(),
        ChocolateColorScheme(),
        RandomColorScheme(),
    )
}

// --- HSB utility ---

/**
 * Convert HSB (hue in degrees, saturation 0-1, brightness 0-1) to Compose Color.
 */
internal fun hsbToColor(hue: Float, saturation: Float, brightness: Float): Color {
    val h = ((hue % 360f) + 360f) % 360f
    val c = brightness * saturation
    val x = c * (1f - ((h / 60f) % 2f - 1f).absoluteValue)
    val m = brightness - c

    val (r, g, b) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    return Color(r + m, g + m, b + m)
}
