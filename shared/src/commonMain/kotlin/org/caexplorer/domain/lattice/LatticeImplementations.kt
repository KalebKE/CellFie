package org.caexplorer.domain.lattice

import org.caexplorer.domain.cell.Cell
import org.caexplorer.domain.cellstate.CellState
import org.caexplorer.domain.util.Coordinate

/**
 * Standard square lattice with Moore neighborhood (8 neighbors).
 *
 * Port of Java SquareLattice.
 */
class SquareLattice(
    width: Int,
    height: Int,
    boundaryCondition: BoundaryCondition = BoundaryCondition.WRAP_AROUND,
    cellFactory: (Coordinate) -> Cell
) : TwoDimensionalLattice(width, height, boundaryCondition) {

    override val displayName = "Square (Moore)"

    override val cells: Array<Cell> = Array(width * height) { i ->
        cellFactory(Coordinate(i / width, i % width))
    }

    // Pre-allocated neighbor array to avoid per-call allocation
    override fun getNeighbors(cell: Cell): Array<Cell> {
        val row = cell.coordinate.row
        val col = cell.coordinate.col

        val up = resolveRow(row - 1)
        val down = resolveRow(row + 1)
        val left = resolveCol(col - 1)
        val right = resolveCol(col + 1)
        val r = resolveRow(row)
        val c = resolveCol(col)

        return arrayOf(
            cells[up * width + left],     // NW
            cells[up * width + c],        // N
            cells[up * width + right],    // NE
            cells[r * width + right],     // E
            cells[down * width + right],  // SE
            cells[down * width + c],      // S
            cells[down * width + left],   // SW
            cells[r * width + left],      // W
        )
    }
}

/**
 * Von Neumann neighborhood (4 neighbors: N, E, S, W).
 */
class VonNeumannLattice(
    width: Int,
    height: Int,
    boundaryCondition: BoundaryCondition = BoundaryCondition.WRAP_AROUND,
    cellFactory: (Coordinate) -> Cell
) : TwoDimensionalLattice(width, height, boundaryCondition) {

    override val displayName = "Square (Von Neumann)"

    override val cells: Array<Cell> = Array(width * height) { i ->
        cellFactory(Coordinate(i / width, i % width))
    }

    override fun getNeighbors(cell: Cell): Array<Cell> {
        val row = cell.coordinate.row
        val col = cell.coordinate.col

        val up = resolveRow(row - 1)
        val down = resolveRow(row + 1)
        val left = resolveCol(col - 1)
        val right = resolveCol(col + 1)

        return arrayOf(
            cells[up * width + col],      // N
            cells[row * width + right],   // E
            cells[down * width + col],    // S
            cells[row * width + left],    // W
        )
    }
}

/**
 * Moore neighborhood with variable radius.
 * Radius 1 = standard 8 neighbors, Radius 2 = 24 neighbors, etc.
 */
class MooreRadiusLattice(
    width: Int,
    height: Int,
    val radius: Int = 1,
    boundaryCondition: BoundaryCondition = BoundaryCondition.WRAP_AROUND,
    cellFactory: (Coordinate) -> Cell
) : TwoDimensionalLattice(width, height, boundaryCondition) {

    override val displayName = "Moore Radius $radius"

    override val cells: Array<Cell> = Array(width * height) { i ->
        cellFactory(Coordinate(i / width, i % width))
    }

    override fun getNeighbors(cell: Cell): Array<Cell> {
        val row = cell.coordinate.row
        val col = cell.coordinate.col
        val neighbors = mutableListOf<Cell>()

        for (dr in -radius..radius) {
            for (dc in -radius..radius) {
                if (dr == 0 && dc == 0) continue
                val r = resolveRow(row + dr)
                val c = resolveCol(col + dc)
                neighbors.add(cells[r * width + c])
            }
        }
        return neighbors.toTypedArray()
    }
}

/**
 * Von Neumann neighborhood with variable radius (diamond-shaped).
 */
class VonNeumannRadiusLattice(
    width: Int,
    height: Int,
    val radius: Int = 1,
    boundaryCondition: BoundaryCondition = BoundaryCondition.WRAP_AROUND,
    cellFactory: (Coordinate) -> Cell
) : TwoDimensionalLattice(width, height, boundaryCondition) {

    override val displayName = "Von Neumann Radius $radius"

    override val cells: Array<Cell> = Array(width * height) { i ->
        cellFactory(Coordinate(i / width, i % width))
    }

    override fun getNeighbors(cell: Cell): Array<Cell> {
        val row = cell.coordinate.row
        val col = cell.coordinate.col
        val neighbors = mutableListOf<Cell>()

        for (dr in -radius..radius) {
            val maxDc = radius - kotlin.math.abs(dr)
            for (dc in -maxDc..maxDc) {
                if (dr == 0 && dc == 0) continue
                val r = resolveRow(row + dr)
                val c = resolveCol(col + dc)
                neighbors.add(cells[r * width + c])
            }
        }
        return neighbors.toTypedArray()
    }
}

/**
 * Hexagonal lattice with 6 neighbors.
 *
 * Port of Java HexagonalLattice. Even rows are shifted right by half a cell.
 */
class HexagonalLattice(
    width: Int,
    height: Int,
    boundaryCondition: BoundaryCondition = BoundaryCondition.WRAP_AROUND,
    cellFactory: (Coordinate) -> Cell
) : TwoDimensionalLattice(width, height, boundaryCondition) {

    override val displayName = "Hexagonal"

    override val cells: Array<Cell> = Array(width * height) { i ->
        cellFactory(Coordinate(i / width, i % width))
    }

    override fun getNeighbors(cell: Cell): Array<Cell> {
        val row = cell.coordinate.row
        val col = cell.coordinate.col
        val isEvenRow = row % 2 == 0

        val up = resolveRow(row - 1)
        val down = resolveRow(row + 1)
        val left = resolveCol(col - 1)
        val right = resolveCol(col + 1)

        return if (isEvenRow) {
            arrayOf(
                cells[up * width + left],      // NW
                cells[up * width + col],        // NE
                cells[row * width + right],     // E
                cells[down * width + col],      // SE
                cells[down * width + left],     // SW
                cells[row * width + left],      // W
            )
        } else {
            arrayOf(
                cells[up * width + col],        // NW
                cells[up * width + right],      // NE
                cells[row * width + right],     // E
                cells[down * width + right],    // SE
                cells[down * width + col],      // SW
                cells[row * width + left],      // W
            )
        }
    }
}

/**
 * Triangular lattice with neighbor relationships based on triangle orientation.
 *
 * Port of Java TriangularLattice.
 */
class TriangularLattice(
    width: Int,
    height: Int,
    boundaryCondition: BoundaryCondition = BoundaryCondition.WRAP_AROUND,
    cellFactory: (Coordinate) -> Cell
) : TwoDimensionalLattice(width, height, boundaryCondition) {

    override val displayName = "Triangular"

    override val cells: Array<Cell> = Array(width * height) { i ->
        cellFactory(Coordinate(i / width, i % width))
    }

    override fun getNeighbors(cell: Cell): Array<Cell> {
        val row = cell.coordinate.row
        val col = cell.coordinate.col
        val isUpTriangle = (row + col) % 2 == 0

        val left = resolveCol(col - 1)
        val right = resolveCol(col + 1)

        return if (isUpTriangle) {
            val down = resolveRow(row + 1)
            arrayOf(
                cells[row * width + left],    // Left
                cells[row * width + right],   // Right
                cells[down * width + col],    // Below
            )
        } else {
            val up = resolveRow(row - 1)
            arrayOf(
                cells[row * width + left],    // Left
                cells[row * width + right],   // Right
                cells[up * width + col],      // Above
            )
        }
    }
}

/**
 * Standard 1D lattice with configurable neighborhood radius.
 *
 * Port of Java StandardOneDimensionalLattice.
 */
/**
 * Standard 1D lattice with configurable neighborhood radius.
 *
 * Port of Java StandardOneDimensionalLattice.
 */
class Standard1DLattice(
    width: Int,
    val radius: Int = 1,
    boundaryCondition: BoundaryCondition = BoundaryCondition.WRAP_AROUND,
    cellFactory: (Coordinate) -> Cell
) : OneDimensionalLattice(width, boundaryCondition) {

    override val displayName = "1D (radius $radius)"

    override val cells: Array<Cell> = Array(width) { i ->
        cellFactory(Coordinate(0, i))
    }

    override fun getNeighbors(cell: Cell): Array<Cell> {
        val col = cell.coordinate.col
        return Array(radius * 2) { i ->
            val offset = if (i < radius) -(radius - i) else (i - radius + 1)
            cells[resolveCol(col + offset)]
        }
    }
}

/**
 * Global lattice where every cell is a neighbor of every other cell.
 */
class GlobalSquareLattice(
    width: Int,
    height: Int,
    boundaryCondition: BoundaryCondition = BoundaryCondition.WRAP_AROUND,
    cellFactory: (Coordinate) -> Cell
) : TwoDimensionalLattice(width, height, boundaryCondition) {

    override val displayName = "Global (all neighbors)"

    override val cells: Array<Cell> = Array(width * height) { i ->
        cellFactory(Coordinate(i / width, i % width))
    }

    override fun getNeighbors(cell: Cell): Array<Cell> {
        val idx = coordinateToIndex(cell.coordinate.row, cell.coordinate.col)
        return Array(cells.size - 1) { i ->
            if (i < idx) cells[i] else cells[i + 1]
        }
    }
}
