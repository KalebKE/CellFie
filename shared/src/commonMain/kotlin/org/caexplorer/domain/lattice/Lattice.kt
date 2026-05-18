package org.caexplorer.domain.lattice

import org.caexplorer.domain.cell.Cell
import org.caexplorer.domain.util.Coordinate

/**
 * Boundary condition for a lattice.
 */
enum class BoundaryCondition {
    /** Edges wrap around to the opposite side (toroidal topology). */
    WRAP_AROUND,
    /** Edges reflect back (mirror at boundaries). */
    REFLECTION
}

/**
 * Defines the topology and neighbor relationships of a cellular automaton.
 *
 * A lattice arranges cells in space and defines which cells are neighbors
 * of which other cells. This is the core abstraction that supports 1D, 2D,
 * hexagonal, triangular, and other topologies.
 *
 * Port of Java Lattice interface.
 */
interface Lattice {
    /** Lattice display name for UI. */
    val displayName: String

    /** All cells in the lattice as a flat array. */
    val cells: Array<Cell>

    /** Number of rows (1 for 1D lattices). */
    val height: Int

    /** Number of columns. */
    val width: Int

    /** Total number of cells. */
    val cellCount: Int get() = cells.size

    /** The boundary condition. */
    val boundaryCondition: BoundaryCondition

    /**
     * Get the neighbors of a cell. This is the HOT PATH — called for every
     * cell on every generation. Must be as fast as possible.
     *
     * @return Array of neighboring cells (excluding the cell itself).
     */
    fun getNeighbors(cell: Cell): Array<Cell>

    /**
     * Get the cell at a specific (row, col) coordinate.
     * Returns null if out of bounds.
     */
    fun getCell(row: Int, col: Int): Cell?

    /**
     * Get the cell at a flat index.
     */
    fun getCell(index: Int): Cell = cells[index]

    /**
     * Convert a flat index to a coordinate.
     */
    fun indexToCoordinate(index: Int): Coordinate

    /**
     * Convert a coordinate to a flat index.
     */
    fun coordinateToIndex(row: Int, col: Int): Int

    /** Whether this is a 1D lattice. */
    val isOneDimensional: Boolean get() = height == 1

    /** Maximum recommended number of neighbors (safety limit). */
    companion object {
        const val MAX_RECOMMENDED_NEIGHBORS = 500
    }
}

/**
 * A 2D lattice base class with cells stored in row-major order.
 */
abstract class TwoDimensionalLattice(
    override val width: Int,
    override val height: Int,
    override val boundaryCondition: BoundaryCondition = BoundaryCondition.WRAP_AROUND
) : Lattice {

    override fun getCell(row: Int, col: Int): Cell? {
        if (row < 0 || row >= height || col < 0 || col >= width) return null
        return cells[row * width + col]
    }

    override fun indexToCoordinate(index: Int): Coordinate =
        Coordinate(index / width, index % width)

    override fun coordinateToIndex(row: Int, col: Int): Int = row * width + col

    /**
     * Wrap-around boundary: coordinates that go off-edge wrap to the opposite side.
     */
    protected fun wrapRow(row: Int): Int = ((row % height) + height) % height
    protected fun wrapCol(col: Int): Int = ((col % width) + width) % width

    /**
     * Reflection boundary: coordinates that go off-edge reflect back.
     */
    protected fun reflectRow(row: Int): Int = when {
        row < 0 -> (-row).coerceAtMost(height - 1)
        row >= height -> (2 * height - row - 2).coerceAtLeast(0)
        else -> row
    }

    protected fun reflectCol(col: Int): Int = when {
        col < 0 -> (-col).coerceAtMost(width - 1)
        col >= width -> (2 * width - col - 2).coerceAtLeast(0)
        else -> col
    }

    /** Resolve a row index based on boundary condition. */
    protected fun resolveRow(row: Int): Int = when (boundaryCondition) {
        BoundaryCondition.WRAP_AROUND -> wrapRow(row)
        BoundaryCondition.REFLECTION -> reflectRow(row)
    }

    /** Resolve a column index based on boundary condition. */
    protected fun resolveCol(col: Int): Int = when (boundaryCondition) {
        BoundaryCondition.WRAP_AROUND -> wrapCol(col)
        BoundaryCondition.REFLECTION -> reflectCol(col)
    }
}

/**
 * A 1D lattice base class.
 */
abstract class OneDimensionalLattice(
    override val width: Int,
    override val boundaryCondition: BoundaryCondition = BoundaryCondition.WRAP_AROUND
) : Lattice {
    override val height: Int = 1

    override fun getCell(row: Int, col: Int): Cell? {
        if (row != 0 || col < 0 || col >= width) return null
        return cells[col]
    }

    override fun indexToCoordinate(index: Int): Coordinate = Coordinate(0, index)
    override fun coordinateToIndex(row: Int, col: Int): Int = col

    protected fun resolveCol(col: Int): Int = when (boundaryCondition) {
        BoundaryCondition.WRAP_AROUND -> ((col % width) + width) % width
        BoundaryCondition.REFLECTION -> when {
            col < 0 -> (-col).coerceAtMost(width - 1)
            col >= width -> (2 * width - col - 2).coerceAtLeast(0)
            else -> col
        }
    }
}
