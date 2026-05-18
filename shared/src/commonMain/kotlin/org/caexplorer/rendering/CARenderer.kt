package org.caexplorer.rendering

/**
 * The shape used to render individual cells.
 */
enum class CellShape {
    SQUARE,
    HEXAGON,
    TRIANGLE
}

/**
 * Platform-abstracted CA renderer interface.
 *
 * Each platform provides an implementation:
 * - Desktop: OpenGL 4.3 via LWJGL
 * - Android: OpenGL ES 3.1
 * - iOS: Metal
 *
 * The renderer receives a flat IntArray of cell RGB colors and draws them
 * to the screen as fast as possible. For integer rules, the GPU can also
 * run compute shaders for the simulation itself.
 */
interface CARenderer {
    /** Initialize the renderer with the given grid dimensions. */
    fun initialize(gridWidth: Int, gridHeight: Int)

    /**
     * Update the cell color buffer.
     * @param cellColors IntArray of ARGB colors, one per cell, in row-major order.
     */
    fun updateCells(cellColors: IntArray)

    /** Trigger a render pass. */
    fun render()

    /** Handle viewport resize. */
    fun resize(viewportWidth: Int, viewportHeight: Int)

    /** Show/hide the cell grid overlay. */
    var gridVisible: Boolean

    /** The cell shape used for rendering. */
    var cellShape: CellShape

    /** Release GPU resources. */
    fun cleanup()

    /** Whether this renderer supports GPU compute (for simulation acceleration). */
    val supportsCompute: Boolean get() = false

    /**
     * If supported, run one simulation step on the GPU using compute shaders.
     * Returns the updated cell states as an IntArray, or null if not supported.
     */
    fun computeStep(currentStates: IntArray, ruleId: Int): IntArray? = null
}
