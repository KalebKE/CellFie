package org.caexplorer.domain

import org.caexplorer.domain.cell.Cell
import org.caexplorer.domain.cellstate.IntegerCellState
import org.caexplorer.domain.util.Coordinate
import kotlin.test.*

class CellTest {

    @Test
    fun initialState() {
        val cell = Cell(IntegerCellState(5), Coordinate(0, 0))
        assertEquals(5, cell.currentState.toInt())
        assertEquals(0, cell.generation)
    }

    @Test
    fun addNewStateAdvancesGeneration() {
        val cell = Cell(IntegerCellState(0), Coordinate(0, 0))
        cell.addNewState(IntegerCellState(1))
        assertEquals(1, cell.currentState.toInt())
        assertEquals(0, cell.previousState?.toInt())
        assertEquals(1, cell.generation)
    }

    @Test
    fun rewindRestoresPreviousState() {
        val cell = Cell(IntegerCellState(0), Coordinate(1, 2))
        cell.addNewState(IntegerCellState(1))
        cell.addNewState(IntegerCellState(2))
        assertEquals(2, cell.currentState.toInt())

        assertTrue(cell.rewind())
        assertEquals(1, cell.currentState.toInt())
        assertEquals(1, cell.generation)
    }

    @Test
    fun rewindAtGenZeroFails() {
        val cell = Cell(IntegerCellState(0), Coordinate(0, 0))
        assertFalse(cell.rewind())
    }

    @Test
    fun resetStateKeepsGeneration() {
        val cell = Cell(IntegerCellState(0), Coordinate(0, 0))
        cell.addNewState(IntegerCellState(1))
        cell.resetState(IntegerCellState(9))
        assertEquals(9, cell.currentState.toInt())
        assertEquals(1, cell.generation)
    }

    @Test
    fun coordinateIsPreserved() {
        val coord = Coordinate(3, 7)
        val cell = Cell(IntegerCellState(0), coord)
        assertEquals(3, cell.coordinate.row)
        assertEquals(7, cell.coordinate.col)
    }
}
