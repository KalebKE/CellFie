package org.caexplorer.domain.util

/**
 * Immutable coordinate representing a cell's position in the lattice.
 * Supports 1D (row=0), 2D (row, col), and 3D (row, col, layer) positions.
 */
data class Coordinate(val row: Int, val col: Int, val layer: Int = 0) {
    override fun toString(): String =
        if (layer == 0) "[$row, $col]" else "[$row, $col, $layer]"
}
