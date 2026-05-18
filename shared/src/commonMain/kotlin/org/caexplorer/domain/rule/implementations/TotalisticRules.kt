package org.caexplorer.domain.rule.implementations

import org.caexplorer.domain.cell.Cell
import org.caexplorer.domain.cellstate.CellState
import org.caexplorer.domain.cellstate.IntegerCellState
import org.caexplorer.domain.rule.IntegerRule
import org.caexplorer.domain.rule.RuleCategory
import kotlin.math.pow

// =============================================================================
// Outer Totalistic Rule (base class)
// =============================================================================

/**
 * Outer totalistic CA rule — the next state depends on the sum of neighbor
 * values and the current cell value, via a lookup table derived from a rule
 * number.
 *
 * The rule number is converted from base-10 to base-numStates, and each digit
 * fills the lookup table indexed by [neighborSum][cellValue].
 *
 * Port of Java OuterTotalistic template.
 */
open class OuterTotalisticRule(
    private val ruleNumber: Long,
    override val numStates: Int = 2,
    override val displayName: String,
    override val description: String,
    override val category: RuleCategory = RuleCategory.TOTALISTIC,
    override val compatibleLatticeNames: List<String> = listOf("Square (Moore)"),
    private val runningAverageDepth: Int = 0,
    private val assumedNeighborCount: Int = 8
) : IntegerRule() {

    // table[neighborSum][cellValue] = nextState
    private val table: Array<IntArray>

    init {
        val maxSum = assumedNeighborCount * (numStates - 1)
        val totalEntries = (maxSum + 1) * numStates
        val digits = convertToBaseN(ruleNumber, numStates, totalEntries)

        table = Array(maxSum + 1) { sum ->
            IntArray(numStates) { cellVal ->
                digits[sum * numStates + cellVal]
            }
        }
    }

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val cellValue = cell.currentState.toInt().coerceIn(0, numStates - 1)
        val neighborSum = neighbors.sumOf { it.currentState.toInt() }
        val clampedSum = neighborSum.coerceIn(0, table.size - 1)

        var result = table[clampedSum][cellValue]

        if (runningAverageDepth > 0) {
            val prev = cell.previousState?.toInt() ?: cellValue
            result = (result + prev) / runningAverageDepth
        }

        return IntegerCellState(result)
    }

    override fun createInitialState(): CellState = IntegerCellState(0)

    companion object {
        fun convertToBaseN(number: Long, base: Int, length: Int): IntArray {
            val result = IntArray(length)
            var remaining = number
            for (i in 0 until length) {
                result[i] = (remaining % base).toInt()
                remaining /= base
            }
            return result
        }
    }
}

// =============================================================================
// 2D Totalistic Rule (base class)
// =============================================================================

/**
 * 2D Totalistic CA rule — the next state depends only on the sum of all cell
 * values in the neighborhood (including the cell itself).
 *
 * The rule number maps each possible sum to a new state via a lookup table.
 * This is the 2D variant; see ElementaryRules.kt for the 1D totalistic rule.
 *
 * Port of Java Totalistic template.
 */
open class Totalistic2DRule(
    private val ruleNumber: Long,
    override val numStates: Int = 2,
    override val displayName: String,
    override val description: String,
    override val category: RuleCategory = RuleCategory.TOTALISTIC,
    override val compatibleLatticeNames: List<String> = listOf("Square (Moore)"),
    private val assumedNeighborCount: Int = 8
) : IntegerRule() {

    // table[totalSum] = nextState
    private val table: IntArray

    init {
        val maxSum = (assumedNeighborCount + 1) * (numStates - 1)
        table = OuterTotalisticRule.convertToBaseN(ruleNumber, numStates, maxSum + 1)
    }

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val cellValue = cell.currentState.toInt()
        val totalSum = (cellValue + neighbors.sumOf { it.currentState.toInt() })
            .coerceIn(0, table.size - 1)
        return IntegerCellState(table[totalSum])
    }

    override fun createInitialState(): CellState = IntegerCellState(0)
}
