package org.caexplorer.domain.lattice

import org.caexplorer.domain.cell.Cell
import org.caexplorer.domain.util.Coordinate

enum class LatticeType(val displayName: String, val description: String) {
    SQUARE_MOORE("Square (Moore)", "8 neighbors - standard grid"),
    SQUARE_VON_NEUMANN("Square (Von Neumann)", "4 neighbors - cross pattern"),
    HEXAGONAL("Hexagonal", "6 neighbors - honeycomb"),
    TRIANGULAR("Triangular", "3 neighbors - triangle mesh"),
    MOORE_RADIUS_2("Moore Radius 2", "24 neighbors - extended"),
    MOORE_RADIUS_3("Moore Radius 3", "48 neighbors - large"),
    VON_NEUMANN_RADIUS_2("Von Neumann Radius 2", "12 neighbors - diamond"),
}

fun createLattice(
    type: LatticeType, width: Int, height: Int, cellFactory: (Coordinate) -> Cell
): Lattice = when (type) {
    LatticeType.SQUARE_MOORE -> SquareLattice(width, height, cellFactory = cellFactory)
    LatticeType.SQUARE_VON_NEUMANN -> VonNeumannLattice(width, height, cellFactory = cellFactory)
    LatticeType.HEXAGONAL -> HexagonalLattice(width, height, cellFactory = cellFactory)
    LatticeType.TRIANGULAR -> TriangularLattice(width, height, cellFactory = cellFactory)
    LatticeType.MOORE_RADIUS_2 -> MooreRadiusLattice(width, height, radius = 2, cellFactory = cellFactory)
    LatticeType.MOORE_RADIUS_3 -> MooreRadiusLattice(width, height, radius = 3, cellFactory = cellFactory)
    LatticeType.VON_NEUMANN_RADIUS_2 -> VonNeumannRadiusLattice(width, height, radius = 2, cellFactory = cellFactory)
}
