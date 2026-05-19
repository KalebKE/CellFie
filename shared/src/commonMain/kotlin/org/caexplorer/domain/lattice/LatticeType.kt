package org.caexplorer.domain.lattice

import org.caexplorer.domain.cell.Cell
import org.caexplorer.domain.util.Coordinate

enum class LatticeType(val displayName: String, val description: String, val is3D: Boolean = false) {
    SQUARE_MOORE("Square (Moore)", "8 neighbors - standard grid"),
    SQUARE_VON_NEUMANN("Square (Von Neumann)", "4 neighbors - cross pattern"),
    HEXAGONAL("Hexagonal", "6 neighbors - honeycomb"),
    TRIANGULAR("Triangular", "3 neighbors - triangle mesh"),
    MOORE_RADIUS_2("Moore Radius 2", "24 neighbors - extended"),
    MOORE_RADIUS_3("Moore Radius 3", "48 neighbors - large"),
    VON_NEUMANN_RADIUS_2("Von Neumann Radius 2", "12 neighbors - diamond"),
    CUBE_MOORE("Cube (Moore)", "26 neighbors - 3D cube", is3D = true),
    CUBE_VON_NEUMANN("Cube (Von Neumann)", "6 neighbors - 3D faces", is3D = true),
}

fun createLattice(
    type: LatticeType, width: Int, height: Int, depth: Int = 1, cellFactory: (Coordinate) -> Cell
): Lattice = when (type) {
    LatticeType.SQUARE_MOORE -> SquareLattice(width, height, cellFactory = cellFactory)
    LatticeType.SQUARE_VON_NEUMANN -> VonNeumannLattice(width, height, cellFactory = cellFactory)
    LatticeType.HEXAGONAL -> HexagonalLattice(width, height, cellFactory = cellFactory)
    LatticeType.TRIANGULAR -> TriangularLattice(width, height, cellFactory = cellFactory)
    LatticeType.MOORE_RADIUS_2 -> MooreRadiusLattice(width, height, radius = 2, cellFactory = cellFactory)
    LatticeType.MOORE_RADIUS_3 -> MooreRadiusLattice(width, height, radius = 3, cellFactory = cellFactory)
    LatticeType.VON_NEUMANN_RADIUS_2 -> VonNeumannRadiusLattice(width, height, radius = 2, cellFactory = cellFactory)
    LatticeType.CUBE_MOORE -> CubeMooreLattice(width, height, depth, cellFactory = cellFactory)
    LatticeType.CUBE_VON_NEUMANN -> CubeVonNeumannLattice(width, height, depth, cellFactory = cellFactory)
}
