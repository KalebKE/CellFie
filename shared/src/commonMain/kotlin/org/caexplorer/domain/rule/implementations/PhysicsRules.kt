package org.caexplorer.domain.rule.implementations

import org.caexplorer.domain.cell.Cell
import org.caexplorer.domain.cellstate.CellState
import org.caexplorer.domain.cellstate.IntegerCellState
import org.caexplorer.domain.rule.IntegerRule
import org.caexplorer.domain.rule.RuleCategory

// =============================================================================
// Q2R Ising Model (Deterministic)
// =============================================================================

/**
 * Deterministic Ising model using the Q2R dynamics. Uses a Von Neumann
 * neighborhood with 2 states (spin up/down). Cells on a checkerboard pattern
 * are updated alternately. A cell flips its spin only when the total
 * neighborhood spin (sum of all neighbors + cell) is exactly zero,
 * conserving energy.
 *
 * Port of Java Q2RIsingModel.
 */
class Q2RIsingModel : IntegerRule() {
    override val numStates = 2
    override val displayName = "Q2R Ising Model"
    override val description = "Deterministic energy-conserving Ising model with Q2R dynamics"
    override val category = RuleCategory.PHYSICS
    override val compatibleLatticeNames = listOf("Square (Von Neumann)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val cellValue = cell.currentState.toInt()
        val row = cell.coordinate.row
        val col = cell.coordinate.col
        val generation = cell.generation

        // Checkerboard: only update cells where (row + col + generation) is even
        val isActivePhase = (row + col + generation) % 2 == 0

        if (!isActivePhase) {
            return IntegerCellState(cellValue)
        }

        // Convert from {0,1} to spin {-1,+1} for energy calculation
        val cellSpin = if (cellValue == 0) -1 else 1
        var totalSpin = cellSpin
        for (n in neighbors) {
            totalSpin += if (n.currentState.toInt() == 0) -1 else 1
        }

        // Flip only when total spin is zero (energy-conserving)
        return if (totalSpin == 0) {
            IntegerCellState(1 - cellValue)
        } else {
            IntegerCellState(cellValue)
        }
    }

    override fun createInitialState(): CellState = IntegerCellState(0)
}

// =============================================================================
// Nucleation (Cyclic CA with trigger=1, 14 states)
// =============================================================================

/**
 * Cyclic CA with 14 states and a trigger threshold of 1 on a Von Neumann
 * neighborhood. A cell advances to the next cyclic state if at least 1
 * neighbor is already in that successor state.
 *
 * Port of Java Nucleation.
 */
class Nucleation : IntegerRule() {
    override val numStates = 14
    override val displayName = "Nucleation"
    override val description = "Cyclic CA nucleation with 14 states and trigger threshold of 1"
    override val category = RuleCategory.TOTALISTIC
    override val compatibleLatticeNames = listOf("Square (Von Neumann)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val cellValue = cell.currentState.toInt()
        val nextValue = (cellValue + 1) % numStates

        val count = neighbors.count { it.currentState.toInt() == nextValue }

        return if (count >= 1) {
            IntegerCellState(nextValue)
        } else {
            IntegerCellState(cellValue)
        }
    }

    override fun createInitialState(): CellState = IntegerCellState(0)
}
