package org.caexplorer.domain.rule.implementations

import org.caexplorer.domain.cell.Cell
import org.caexplorer.domain.cellstate.CellState
import org.caexplorer.domain.cellstate.IntegerCellState
import org.caexplorer.domain.rule.IntegerRule
import org.caexplorer.domain.rule.Rule
import org.caexplorer.domain.rule.RuleCategory
import org.caexplorer.domain.rule.RuleProperty
import kotlin.math.roundToInt
import kotlin.random.Random

// =============================================================================
// Integer Average
// =============================================================================

/**
 * Averages the cell value and all neighbor values, rounding to the nearest
 * integer. Creates smooth blurring/diffusion effects.
 *
 * Port of Java IntegerAverage.
 */
class IntegerAverage(override val numStates: Int = 10) : IntegerRule() {
    override val displayName = "Integer Average"
    override val description = "Averages cell and neighbor values with integer rounding"
    override val category = RuleCategory.TOTALISTIC
    override val compatibleLatticeNames = listOf("Square (Moore)", "Square (Von Neumann)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val cellValue = cell.currentState.toInt()
        val sum = cellValue + neighbors.sumOf { it.currentState.toInt() }
        val avg = (sum.toDouble() / (neighbors.size + 1)).roundToInt()
        return IntegerCellState(avg.coerceIn(0, numStates - 1))
    }

    override fun createInitialState(): CellState = IntegerCellState(0)

    override val properties get() = listOf(
        RuleProperty.IntProperty("numStates", "States", numStates, 2, 256, "Number of cell states")
    )
    override fun withProperty(key: String, value: Any): Rule = when (key) {
        "numStates" -> IntegerAverage((value as Number).toInt().coerceIn(2, 256))
        else -> this
    }
}

// =============================================================================
// Integer Sort
// =============================================================================

/**
 * Sorting-inspired rule. The cell compares its value to each neighbor: if
 * the cell is larger than the majority of its neighbors it decreases; if
 * smaller it increases. This produces a smoothing/sorting effect adapted
 * from the Margolus block-based sorting approach.
 *
 * Port of Java IntegerSort (adapted from Margolus 1D to neighbor-based).
 */
class IntegerSort(override val numStates: Int = 10) : IntegerRule() {
    override val displayName = "Integer Sort"
    override val description = "Sorting-based rule where cells move toward local order"
    override val category = RuleCategory.OTHER
    override val compatibleLatticeNames = listOf("Square (Moore)", "Square (Von Neumann)", "1D (radius 1)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val cellValue = cell.currentState.toInt()

        var greaterCount = 0
        var lessCount = 0
        for (n in neighbors) {
            val nVal = n.currentState.toInt()
            if (nVal > cellValue) greaterCount++
            else if (nVal < cellValue) lessCount++
        }

        val result = when {
            greaterCount > lessCount -> (cellValue + 1).coerceAtMost(numStates - 1)
            lessCount > greaterCount -> (cellValue - 1).coerceAtLeast(0)
            else -> cellValue
        }

        return IntegerCellState(result)
    }

    override fun createInitialState(): CellState = IntegerCellState(0)

    override val properties get() = listOf(
        RuleProperty.IntProperty("numStates", "States", numStates, 2, 256, "Number of cell states")
    )
    override fun withProperty(key: String, value: Any): Rule = when (key) {
        "numStates" -> IntegerSort((value as Number).toInt().coerceIn(2, 256))
        else -> this
    }
}

// =============================================================================
// Copy Random Neighbor
// =============================================================================

/**
 * Each cell copies the state of a randomly chosen neighbor.
 * Creates voter-model-like dynamics.
 *
 * Port of Java CopyRandomNeighbor.
 */
class CopyRandomNeighbor(override val numStates: Int = 10) : IntegerRule() {
    override val displayName = "Copy Random Neighbor"
    override val description = "Each cell copies the state of a randomly selected neighbor"
    override val category = RuleCategory.PROBABILISTIC
    override val compatibleLatticeNames = listOf("Square (Moore)", "Square (Von Neumann)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        if (neighbors.isEmpty()) return IntegerCellState(cell.currentState.toInt())
        val chosen = neighbors[Random.nextInt(neighbors.size)]
        return IntegerCellState(chosen.currentState.toInt())
    }

    override fun createInitialState(): CellState = IntegerCellState(0)

    override val properties get() = listOf(
        RuleProperty.IntProperty("numStates", "States", numStates, 2, 256, "Number of cell states")
    )
    override fun withProperty(key: String, value: Any): Rule = when (key) {
        "numStates" -> CopyRandomNeighbor((value as Number).toInt().coerceIn(2, 256))
        else -> this
    }
}

// =============================================================================
// Sum Modulo N
// =============================================================================

/**
 * Computes the sum of the cell and all neighbors, then takes the result
 * modulo the number of states. Produces fractal-like growth patterns.
 *
 * Port of Java SumModuloN.
 */
class SumModuloN(override val numStates: Int = 8) : IntegerRule() {
    override val displayName = "Sum Modulo N"
    override val description = "Sum of cell and neighbors modulo the number of states"
    override val category = RuleCategory.TOTALISTIC
    override val compatibleLatticeNames = listOf("Square (Moore)", "Square (Von Neumann)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val sum = cell.currentState.toInt() + neighbors.sumOf { it.currentState.toInt() }
        return IntegerCellState(sum % numStates)
    }

    override fun createInitialState(): CellState = IntegerCellState(0)

    override val properties get() = listOf(
        RuleProperty.IntProperty("numStates", "States", numStates, 2, 256, "Number of cell states")
    )
    override fun withProperty(key: String, value: Any): Rule = when (key) {
        "numStates" -> SumModuloN((value as Number).toInt().coerceIn(2, 256))
        else -> this
    }
}
