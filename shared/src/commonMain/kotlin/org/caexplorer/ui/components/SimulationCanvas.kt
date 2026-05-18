package org.caexplorer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.min

/**
 * High-performance Compose Canvas for rendering cellular automaton grids.
 *
 * Uses the "fast color array" strategy:
 * 1. Engine provides IntArray of ARGB colors (one per cell, row-major)
 * 2. We create an ImageBitmap (1 pixel per cell) via platform-specific code
 * 3. Compose draws this bitmap scaled to fill the viewport with nearest-neighbor filtering
 *
 * This matches the original Java approach (int[] → BufferedImage → Graphics2D.drawImage with scaling)
 * and achieves the same performance characteristics.
 *
 * Supports:
 * - Pinch/scroll zoom
 * - Pan/drag
 * - Grid overlay at sufficient zoom level
 */
@Composable
fun SimulationCanvas(
    cellColors: IntArray,
    gridWidth: Int,
    gridHeight: Int,
    gridVisible: Boolean = false,
    fitToWindowTrigger: Int = 0,
    modifier: Modifier = Modifier,
    onCellClick: ((col: Int, row: Int) -> Unit)? = null
) {
    // Zoom and pan state
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Reset zoom/pan when fitToWindowTrigger changes
    LaunchedEffect(fitToWindowTrigger) {
        if (fitToWindowTrigger > 0) {
            scale = 1f
            offset = Offset.Zero
        }
    }

    // Create ImageBitmap from cell colors (only when data changes)
    val bitmap = remember(cellColors, gridWidth, gridHeight) {
        if (gridWidth <= 0 || gridHeight <= 0 || cellColors.isEmpty()) null
        else createCellBitmap(cellColors, gridWidth, gridHeight)
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.1f, 100f)
                    offset += pan
                }
            }
    ) {
        if (bitmap == null) return@Canvas

        val canvasWidth = size.width
        val canvasHeight = size.height

        // Calculate scale to fit grid in viewport
        val fitScaleX = canvasWidth / gridWidth.toFloat()
        val fitScaleY = canvasHeight / gridHeight.toFloat()
        val fitScale = min(fitScaleX, fitScaleY)

        val totalScale = fitScale * scale

        // Center the grid
        val gridPixelWidth = gridWidth * totalScale
        val gridPixelHeight = gridHeight * totalScale
        val centerOffsetX = (canvasWidth - gridPixelWidth) / 2f + offset.x
        val centerOffsetY = (canvasHeight - gridPixelHeight) / 2f + offset.y

        // Draw the bitmap scaled with nearest-neighbor filtering (pixel-perfect cells)
        drawImage(
            image = bitmap,
            dstOffset = IntOffset(
                centerOffsetX.toInt(),
                centerOffsetY.toInt()
            ),
            dstSize = IntSize(
                gridPixelWidth.toInt().coerceAtLeast(1),
                gridPixelHeight.toInt().coerceAtLeast(1)
            ),
            filterQuality = FilterQuality.None // Nearest-neighbor for crisp cells
        )

        // Draw grid overlay when zoomed in sufficiently
        if (gridVisible && totalScale >= 4f) {
            val gridColor = Color(0x40000000)

            for (col in 0..gridWidth) {
                val x = centerOffsetX + col * totalScale
                drawLine(
                    color = gridColor,
                    start = Offset(x, centerOffsetY),
                    end = Offset(x, centerOffsetY + gridPixelHeight),
                    strokeWidth = 1f
                )
            }
            for (row in 0..gridHeight) {
                val y = centerOffsetY + row * totalScale
                drawLine(
                    color = gridColor,
                    start = Offset(centerOffsetX, y),
                    end = Offset(centerOffsetX + gridPixelWidth, y),
                    strokeWidth = 1f
                )
            }
        }
    }
}

/**
 * Platform-specific: Creates an ImageBitmap from a flat ARGB color array.
 * One pixel per cell, later scaled by Canvas for display.
 */
expect fun createCellBitmap(colors: IntArray, width: Int, height: Int): ImageBitmap

