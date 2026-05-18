package org.caexplorer.domain.rule.implementations

import org.caexplorer.domain.cell.Cell
import org.caexplorer.domain.cellstate.CellState
import org.caexplorer.domain.cellstate.IntegerCellState
import org.caexplorer.domain.rule.IntegerRule
import org.caexplorer.domain.rule.RuleCategory
import org.caexplorer.domain.util.BaseConverter

/**
 * Wolfram elementary cellular automaton rules (1D, radius 1, 2 states).
 *
 * Rule number (0-255) encodes all 8 possible 3-cell neighborhood → next state mappings.
 * E.g., Rule 30 is one of the most studied chaotic CAs.
 *
 * Port of Java WolframRuleNumber.
 */
class WolframRule(val ruleNumber: Int) : IntegerRule() {
    override val numStates = 2
    override val displayName = "Rule $ruleNumber"
    override val description = "Wolfram elementary CA Rule $ruleNumber"
    override val category = RuleCategory.ELEMENTARY
    override val compatibleLatticeNames = listOf("1D (radius 1)")

    private val lookupTable: IntArray = IntArray(8) { i ->
        (ruleNumber shr i) and 1
    }

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val left = neighbors[0].currentState.toInt()
        val center = cell.currentState.toInt()
        val right = neighbors[1].currentState.toInt()

        val index = (left shl 2) or (center shl 1) or right
        return IntegerCellState(lookupTable[index])
    }

    override fun createInitialState(): CellState = IntegerCellState(0)

    companion object {
        val RULE_30 = WolframRule(30)
        val RULE_90 = WolframRule(90)
        val RULE_110 = WolframRule(110)
        val RULE_184 = WolframRule(184)
    }
}

/**
 * Generalized totalistic rule for multi-state 1D CAs.
 */
class TotalisticRule(
    override val numStates: Int,
    val radius: Int,
    val ruleNumber: Long,
) : IntegerRule() {
    override val displayName = "Totalistic ($numStates states, radius $radius, rule $ruleNumber)"
    override val description = "Totalistic 1D rule with $numStates states"
    override val category = RuleCategory.TOTALISTIC
    override val compatibleLatticeNames = listOf("1D (radius $radius)")

    private val lookupTable: IntArray

    init {
        val neighborhoodSize = 2 * radius + 1
        val maxSum = (numStates - 1) * neighborhoodSize
        val digits = BaseConverter.convertFromBaseTen(ruleNumber, numStates)
        lookupTable = IntArray(maxSum + 1) { i ->
            if (i < digits.size) digits[i] else 0
        }
    }

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        var sum = cell.currentState.toInt()
        for (n in neighbors) sum += n.currentState.toInt()
        val nextVal = lookupTable.getOrElse(sum) { 0 }
        return IntegerCellState(nextVal)
    }

    override fun createInitialState(): CellState = IntegerCellState(0)
}
