package org.caexplorer.domain.util

/**
 * Immutable 2D coordinate representing a cell's position in the lattice.
 */
data class Coordinate(val row: Int, val col: Int) {
    override fun toString(): String = "[$row, $col]"
}
