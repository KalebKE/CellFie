package org.caexplorer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * High-performance Compose Canvas for rendering cellular automaton grids.
 *
 * Uses the "fast color array" strategy:
 * 1. Engine provides IntArray of ARGB colors (one per cell, row-major)
 * 2. We create an ImageBitmap (1 pixel per cell) via platform-specific code
 * 3. Compose draws this bitmap scaled to fill the viewport with nearest-neighbor filtering
 *
 * Supports:
 * - Pinch/scroll zoom
 * - Pan/drag
 * - Grid overlay at sufficient zoom level
 * - Draw mode: click/drag to paint cells
 */
@Composable
fun SimulationCanvas(
    cellColors: IntArray,
    gridWidth: Int,
    gridHeight: Int,
    gridVisible: Boolean = false,
    fitToWindowTrigger: Int = 0,
    drawMode: Boolean = false,
    onCellToggle: ((col: Int, row: Int) -> Unit)? = null,
    onCellPaint: ((col: Int, row: Int) -> Unit)? = null,
    onPaintFinished: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    onCellClick: ((col: Int, row: Int) -> Unit)? = null
) {
    // Zoom and pan state
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Canvas size tracked for coordinate conversion
    var canvasSize by remember { mutableStateOf(Offset.Zero) }

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

    // Coordinate conversion: screen position -> grid (col, row)
    fun screenToCell(screenPos: Offset): Pair<Int, Int>? {
        if (gridWidth <= 0 || gridHeight <= 0) return null
        val cw = canvasSize.x
        val ch = canvasSize.y
        if (cw <= 0f || ch <= 0f) return null

        val fitScaleX = cw / gridWidth.toFloat()
        val fitScaleY = ch / gridHeight.toFloat()
        val fitScale = min(fitScaleX, fitScaleY)
        val totalScale = fitScale * scale

        val gridPixelWidth = gridWidth * totalScale
        val gridPixelHeight = gridHeight * totalScale
        val centerOffsetX = (cw - gridPixelWidth) / 2f + offset.x
        val centerOffsetY = (ch - gridPixelHeight) / 2f + offset.y

        val col = ((screenPos.x - centerOffsetX) / totalScale).toInt()
        val row = ((screenPos.y - centerOffsetY) / totalScale).toInt()

        if (col < 0 || col >= gridWidth || row < 0 || row >= gridHeight) return null
        return Pair(col, row)
    }

    // Track hover position for crosshair in draw mode
    var hoverPosition by remember { mutableStateOf<Offset?>(null) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .shadow(2.dp)
            .border(1.dp, Color(0x20000000))
            .pointerInput(drawMode) {
                if (drawMode) {
                    // Draw mode: click/drag paints cells
                    var lastPaintedCell: Pair<Int, Int>? = null
                    detectDragGestures(
                        onDragStart = { startPos ->
                            lastPaintedCell = null
                            hoverPosition = startPos
                            val cell = screenToCell(startPos)
                            if (cell != null) {
                                onCellToggle?.invoke(cell.first, cell.second)
                                lastPaintedCell = cell
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            hoverPosition = change.position
                            val cell = screenToCell(change.position)
                            if (cell != null && cell != lastPaintedCell) {
                                onCellPaint?.invoke(cell.first, cell.second)
                                lastPaintedCell = cell
                            }
                        },
                        onDragEnd = {
                            lastPaintedCell = null
                        },
                        onDragCancel = {
                            lastPaintedCell = null
                        }
                    )
                } else {
                    // Pan/zoom mode
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.1f, 100f)
                        offset += pan
                    }
                }
            }
    ) {
        // Track canvas size for coordinate conversion
        canvasSize = Offset(size.width, size.height)

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
        // Draw crosshair cursor in draw mode
        if (drawMode && hoverPosition != null) {
            val pos = hoverPosition!!
            val crosshairColor = Color(0x80FFFFFF)
            val crosshairSize = 12f
            drawLine(
                color = crosshairColor,
                start = Offset(pos.x - crosshairSize, pos.y),
                end = Offset(pos.x + crosshairSize, pos.y),
                strokeWidth = 1.5f
            )
            drawLine(
                color = crosshairColor,
                start = Offset(pos.x, pos.y - crosshairSize),
                end = Offset(pos.x, pos.y + crosshairSize),
                strokeWidth = 1.5f
            )
        }
    }
}

/**
 * Platform-specific: Creates an ImageBitmap from a flat ARGB color array.
 * One pixel per cell, later scaled by Canvas for display.
 */
expect fun createCellBitmap(colors: IntArray, width: Int, height: Int): ImageBitmap

