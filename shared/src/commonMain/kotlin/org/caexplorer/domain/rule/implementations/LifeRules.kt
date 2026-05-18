package org.caexplorer.domain.rule.implementations

import org.caexplorer.domain.cell.Cell
import org.caexplorer.domain.cellstate.CellState
import org.caexplorer.domain.cellstate.IntegerCellState
import org.caexplorer.domain.rule.BinaryRule
import org.caexplorer.domain.rule.RuleCategory

/**
 * Conway's Game of Life.
 *
 * Rules:
 * - A live cell with 2 or 3 live neighbors survives.
 * - A dead cell with exactly 3 live neighbors becomes alive.
 * - All other cells die or stay dead.
 *
 * Port of Java Life rule.
 */
class Life : BinaryRule() {
    override val displayName = "Life"
    override val description = "Conway's Game of Life (B3/S23)"
    override val category = RuleCategory.LIFE_LIKE
    override val compatibleLatticeNames = listOf("Square (Moore)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val alive = cell.currentState.toInt() != 0
        var liveNeighbors = 0
        for (n in neighbors) {
            if (n.currentState.toInt() != 0) liveNeighbors++
        }

        val nextAlive = if (alive) {
            liveNeighbors == 2 || liveNeighbors == 3
        } else {
            liveNeighbors == 3
        }

        return IntegerCellState(if (nextAlive) 1 else 0)
    }

    override fun createInitialState(): CellState = IntegerCellState(0)
}

/**
 * Generalized Life-like rule using B/S notation.
 * B = birth counts, S = survival counts.
 *
 * Examples: B3/S23 = Life, B36/S23 = HighLife, B3/S012345678 = Maze
 */
class LifeLike(
    private val birth: Set<Int>,
    private val survival: Set<Int>,
    override val displayName: String = "B${birth.sorted().joinToString("")}/S${survival.sorted().joinToString("")}"
) : BinaryRule() {
    override val description = "Life-like rule $displayName"
    override val category = RuleCategory.LIFE_LIKE
    override val compatibleLatticeNames = listOf("Square (Moore)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val alive = cell.currentState.toInt() != 0
        var liveNeighbors = 0
        for (n in neighbors) {
            if (n.currentState.toInt() != 0) liveNeighbors++
        }

        val nextAlive = if (alive) {
            liveNeighbors in survival
        } else {
            liveNeighbors in birth
        }

        return IntegerCellState(if (nextAlive) 1 else 0)
    }

    override fun createInitialState(): CellState = IntegerCellState(0)

    companion object {
        val HIGHLIFE = LifeLike(setOf(3, 6), setOf(2, 3), "HighLife")
        val SEEDS = LifeLike(setOf(2), emptySet(), "Seeds")
        val DAY_AND_NIGHT = LifeLike(setOf(3, 6, 7, 8), setOf(3, 4, 6, 7, 8), "Day & Night")
        val DIAMOEBA = LifeLike(setOf(3, 5, 6, 7, 8), setOf(5, 6, 7, 8), "Diamoeba")
        val LONG_LIFE = LifeLike(setOf(3, 4, 5), setOf(5), "Long Life")
        val STAINS = LifeLike(setOf(3, 6, 7, 8), setOf(2, 3, 5, 6, 7, 8), "Stains")
        val REPLICATOR = LifeLike(setOf(1, 3, 5, 7), setOf(1, 3, 5, 7), "Replicator")
    }
}
