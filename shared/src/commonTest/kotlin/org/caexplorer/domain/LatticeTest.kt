package org.caexplorer.domain

import org.caexplorer.domain.cell.Cell
import org.caexplorer.domain.cellstate.IntegerCellState
import org.caexplorer.domain.lattice.*
import org.caexplorer.domain.util.Coordinate
import kotlin.test.*

class LatticeTest {

    private fun makeCell(coord: Coordinate) = Cell(IntegerCellState(0), coord)

    @Test
    fun squareLatticeDimensions() {
        val lattice = SquareLattice(10, 20, cellFactory = ::makeCell)
        assertEquals(10, lattice.width)
        assertEquals(20, lattice.height)
        assertEquals(200, lattice.cellCount)
    }

    @Test
    fun squareLatticeMooreNeighbors() {
        val lattice = SquareLattice(5, 5, cellFactory = ::makeCell)
        val center = lattice.getCell(2, 2)!!
        val neighbors = lattice.getNeighbors(center)
        assertEquals(8, neighbors.size)
    }

    @Test
    fun squareLatticeWrapAround() {
        val lattice = SquareLattice(5, 5, BoundaryCondition.WRAP_AROUND, ::makeCell)
        val corner = lattice.getCell(0, 0)!!
        val neighbors = lattice.getNeighbors(corner)
        assertEquals(8, neighbors.size)
        // Top-left corner wraps: NW neighbor should be (4,4)
        val nw = neighbors[0]
        assertEquals(4, nw.coordinate.row)
        assertEquals(4, nw.coordinate.col)
    }

    @Test
    fun vonNeumannNeighbors() {
        val lattice = VonNeumannLattice(5, 5, cellFactory = ::makeCell)
        val center = lattice.getCell(2, 2)!!
        val neighbors = lattice.getNeighbors(center)
        assertEquals(4, neighbors.size)
    }

    @Test
    fun hexagonalNeighbors() {
        val lattice = HexagonalLattice(10, 10, cellFactory = ::makeCell)
        val center = lattice.getCell(5, 5)!!
        val neighbors = lattice.getNeighbors(center)
        assertEquals(6, neighbors.size)
    }

    @Test
    fun triangularNeighbors() {
        val lattice = TriangularLattice(10, 10, cellFactory = ::makeCell)
        val center = lattice.getCell(3, 3)!!
        val neighbors = lattice.getNeighbors(center)
        assertEquals(3, neighbors.size)
    }

    @Test
    fun mooreRadiusNeighbors() {
        val lattice = MooreRadiusLattice(10, 10, radius = 2, cellFactory = ::makeCell)
        val center = lattice.getCell(5, 5)!!
        val neighbors = lattice.getNeighbors(center)
        assertEquals(24, neighbors.size) // (2*2+1)^2 - 1 = 24
    }

    @Test
    fun oneDimensionalLattice() {
        val lattice = Standard1DLattice(20, radius = 1, cellFactory = ::makeCell)
        assertEquals(1, lattice.height)
        assertEquals(20, lattice.width)
        assertTrue(lattice.isOneDimensional)
        val neighbors = lattice.getNeighbors(lattice.getCell(0, 5)!!)
        assertEquals(2, neighbors.size)
    }

    @Test
    fun getCellOutOfBoundsReturnsNull() {
        val lattice = SquareLattice(5, 5, cellFactory = ::makeCell)
        assertNull(lattice.getCell(-1, 0))
        assertNull(lattice.getCell(5, 0))
        assertNull(lattice.getCell(0, 5))
    }

    @Test
    fun coordinateConversion() {
        val lattice = SquareLattice(10, 10, cellFactory = ::makeCell)
        val coord = lattice.indexToCoordinate(35)
        assertEquals(3, coord.row)
        assertEquals(5, coord.col)
        assertEquals(35, lattice.coordinateToIndex(3, 5))
    }
}
