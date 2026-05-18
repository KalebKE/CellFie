package org.caexplorer.domain.colorscheme

import androidx.compose.ui.graphics.Color

/**
 * Maps cell state values to display colors.
 *
 * Color schemes define the visual palette for CA rendering. Each scheme maps
 * an integer state (or percentage) to a Color, with configurable empty/filled
 * colors and gradient interpolation for intermediate values.
 *
 * Port of Java ColorScheme hierarchy.
 */
interface ColorScheme {
    /** Display name for the UI. */
    val displayName: String

    /** Color for state 0 (empty cells). */
    var emptyColor: Color

    /** Color for state numStates-1 (filled cells). */
    var filledColor: Color

    /** Color at the gradient start (for intermediate states). */
    var emptyGradientColor: Color

    /** Color at the gradient end (for intermediate states). */
    var filledGradientColor: Color

    /**
     * Get color for a fractional value in [0.0, 1.0].
     * 0.0 → emptyColor, 1.0 → filledColor, intermediate → gradient.
     */
    fun getColor(percent: Double): Color {
        if (percent <= 0.0) return emptyColor
        if (percent >= 1.0) return filledColor
        return interpolate(emptyGradientColor, filledGradientColor, percent.toFloat())
    }

    /**
     * Get color for an integer state in [0, numStates).
     */
    fun getColor(state: Int, numStates: Int): Color {
        if (numStates <= 1) return emptyColor
        if (state == 0) return emptyColor
        if (state == numStates - 1) return filledColor
        val percent = state.toDouble() / (numStates - 1).toDouble()
        return getColor(percent)
    }

    /**
     * Colors used for tagged cell overlays. Returns a rotating set.
     */
    fun getTaggingColor(index: Int): Color {
        return DEFAULT_TAGGING_COLORS[index % DEFAULT_TAGGING_COLORS.size]
    }

    companion object {
        val DEFAULT_EMPTY_COLOR = Color(0, 0, 102)       // Dark blue
        val DEFAULT_FILLED_COLOR = Color(191, 191, 255)   // Light blue

        val DEFAULT_TAGGING_COLORS = listOf(
            Color(0, 255, 0),       // green
            Color(0, 0, 255),       // blue
            Color(160, 32, 240),    // purple
            Color(64, 224, 208),    // turquoise
            Color(0, 0, 128),       // navy blue
            Color(34, 139, 34),     // forest green
            Color(148, 0, 211),     // dark violet
            Color(0, 255, 255),     // cyan
            Color(127, 255, 212),   // aquamarine
            Color(127, 255, 0),     // chartreuse
            Color(50, 205, 50),     // lime green
            Color(255, 110, 180),   // hot pink
        )

        const val TAGGED_ALPHA = 200

        /** Porter-Duff alpha blending for tagged color overlay. */
        fun blendTaggedColor(original: Color, tagging: Color): Color {
            val alpha = TAGGED_ALPHA / 255f
            val r = original.red * (1 - alpha) + tagging.red * alpha
            val g = original.green * (1 - alpha) + tagging.green * alpha
            val b = original.blue * (1 - alpha) + tagging.blue * alpha
            return Color(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f))
        }

        /** Linear interpolation between two colors. */
        fun interpolate(from: Color, to: Color, fraction: Float): Color {
            val r = from.red + (to.red - from.red) * fraction
            val g = from.green + (to.green - from.green) * fraction
            val b = from.blue + (to.blue - from.blue) * fraction
            return Color(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f))
        }
    }
}

/**
 * Base implementation for gradient-based color schemes.
 */
abstract class GradientColorScheme(
    override var emptyColor: Color,
    override var filledColor: Color,
    override var emptyGradientColor: Color = emptyColor,
    override var filledGradientColor: Color = filledColor
) : ColorScheme
