package org.caexplorer.domain.rule.implementations

import org.caexplorer.domain.cell.Cell
import org.caexplorer.domain.cellstate.CellState
import org.caexplorer.domain.cellstate.IntegerCellState
import org.caexplorer.domain.cellstate.RealValuedState
import org.caexplorer.domain.rule.BinaryRule
import org.caexplorer.domain.rule.IntegerRule
import org.caexplorer.domain.rule.RealRule
import org.caexplorer.domain.rule.RuleCategory
import kotlin.math.E
import kotlin.math.floor
import kotlin.math.pow
import kotlin.random.Random

// =============================================================================
// Cyclic Pulse (Real-valued cyclic increasing)
// =============================================================================

/**
 * Real-valued rule where cells increase by 10 each step, or by 20 if more
 * than 4 neighbors have values at least 20 larger. Result is modulo 1000.
 *
 * Port of Java CyclicPulse.
 */
class CyclicPulse : RealRule() {
    override val displayName = "Cyclic Pulse"
    override val description = "Cells increase in value with pulses when surrounded by larger neighbors"
    override val category = RuleCategory.CONTINUOUS
    override val compatibleLatticeNames = listOf("Square (Moore)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val cellVal = (cell.currentState as? RealValuedState)?.state ?: cell.currentState.toInt().toDouble()

        var count = 0
        for (n in neighbors) {
            val nVal = (n.currentState as? RealValuedState)?.state ?: n.currentState.toInt().toDouble()
            if (nVal > cellVal + 20.0) {
                count++
            }
        }

        var result = if (count > 4) {
            cellVal + 20.0
        } else {
            cellVal + 10.0
        }

        if (result > MAX_VALUE) {
            result = (result / MAX_VALUE - floor(result / MAX_VALUE)) * MAX_VALUE
        }

        return RealValuedState(result, 0.0, MAX_VALUE)
    }

    override fun createInitialState(): CellState = RealValuedState(0.0, 0.0, MAX_VALUE)

    companion object {
        private const val MAX_VALUE = 1000.0
    }
}

// =============================================================================
// Electric Loops (Outer Totalistic 301 with running average)
// =============================================================================

/**
 * Outer totalistic rule 301 with 2 states and running average of depth 2.
 * Produces electric loop-like patterns.
 *
 * Port of Java ElectricLoops.
 */
class ElectricLoops : OuterTotalisticRule(
    ruleNumber = 301,
    numStates = 2,
    displayName = "Electric Loops",
    description = "Electric loop patterns using outer totalistic rule 301 with running average",
    category = RuleCategory.TOTALISTIC,
    compatibleLatticeNames = listOf("Square (Moore)"),
    runningAverageDepth = 2
)

// =============================================================================
// Super Loops (Outer Totalistic 429 with running average)
// =============================================================================

/**
 * Outer totalistic rule 429 with 2 states and running average of depth 2.
 * Produces persistent loop structures.
 *
 * Port of Java SuperLoops.
 */
class SuperLoops : OuterTotalisticRule(
    ruleNumber = 429,
    numStates = 2,
    displayName = "Super Loops",
    description = "Persistent loop structures using outer totalistic rule 429 with running average",
    category = RuleCategory.TOTALISTIC,
    compatibleLatticeNames = listOf("Square (Moore)"),
    runningAverageDepth = 2
)

// =============================================================================
// Galactic Flash Web (Real-valued increasing with threshold)
// =============================================================================

/**
 * Real-valued rule where cells always increase by 10, but increase by 20
 * if surrounded by 6 or more neighbors with values at least 20 larger.
 * Result is modulo 1000.
 *
 * Port of Java GalacticFlashWeb.
 */
class GalacticFlashWeb : RealRule() {
    override val displayName = "Galactic Flash Web"
    override val description = "Flashbulb or pulsing nebulous web depending on lattice type"
    override val category = RuleCategory.CONTINUOUS
    override val compatibleLatticeNames = listOf("Square (Moore)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val cellVal = (cell.currentState as? RealValuedState)?.state ?: cell.currentState.toInt().toDouble()

        var count = 0
        for (n in neighbors) {
            val nVal = (n.currentState as? RealValuedState)?.state ?: n.currentState.toInt().toDouble()
            if (nVal > cellVal + 20.0) {
                count++
            }
        }

        var result = if (count > 6) {
            cellVal + 20.0
        } else {
            cellVal + 10.0
        }

        if (result > MAX_VALUE) {
            result = (result / MAX_VALUE - floor(result / MAX_VALUE)) * MAX_VALUE
        }

        return RealValuedState(result, 0.0, MAX_VALUE)
    }

    override fun createInitialState(): CellState = RealValuedState(0.0, 0.0, MAX_VALUE)

    companion object {
        private const val MAX_VALUE = 1000.0
    }
}

// =============================================================================
// Selfish CA (Binary, neighborhood-indexed rule)
// =============================================================================

/**
 * "Mostly Selfish" CA where the nearest neighbors and cell form a binary
 * number that determines which neighbor's value to copy. The combined
 * neighborhood array (neighbors with cell in the middle) is indexed by
 * the base-2 value of the nearest three cells (left, cell, right).
 *
 * Port of Java SelfishCA.
 */
class SelfishCA : BinaryRule() {
    override val displayName = "Mostly Selfish"
    override val description = "Neighbors determine which rule is applied to each cell"
    override val category = RuleCategory.OTHER
    override val compatibleLatticeNames = listOf("1D (radius 1)", "Square (Moore)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val cellValue = cell.currentState.toInt().coerceIn(0, 1)

        // Build combined array: neighbors with cell in the middle
        val middle = neighbors.size / 2
        val combined = IntArray(neighbors.size + 1)
        for (i in 0 until middle) {
            combined[i] = neighbors[i].currentState.toInt().coerceIn(0, 1)
        }
        combined[middle] = cellValue
        for (i in (middle + 1) until combined.size) {
            combined[i] = neighbors[i - 1].currentState.toInt().coerceIn(0, 1)
        }

        // Get nearest neighbors to cell (left and right of middle)
        val left = if (middle > 0) combined[middle - 1] else 0
        val right = if (middle + 1 < combined.size) combined[middle + 1] else 0

        // Convert left-cell-right to base-2 number
        val base2 = left * 4 + cellValue * 2 + right

        // Index from right side of combined array
        var pos = (combined.size - 1) - base2

        // Modulo wrap
        pos = ((pos % combined.size) + combined.size) % combined.size

        return IntegerCellState(combined[pos])
    }

    override fun createInitialState(): CellState = IntegerCellState(0)
}

// =============================================================================
// Obesity Model (Probabilistic social influence)
// =============================================================================

/**
 * A social influence model where each cell represents an individual with a
 * BMI category (0=underweight, 1=normal, 2=overweight, 3=obese). Each cell
 * probabilistically adopts a state based on the proportion of neighbors in
 * each state, modified by social temperature.
 *
 * Simplified port of Java ObesityModel (without slider UI).
 */
class ObesityModel(
    override val numStates: Int = 4,
    private val socialTemperature: Double = 1.0
) : IntegerRule() {
    override val displayName = "Obesity Model"
    override val description = "Social influence model of obesity spread through networks"
    override val category = RuleCategory.SOCIAL
    override val compatibleLatticeNames = listOf("Square (Moore)", "Square (Von Neumann)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val cellValue = cell.currentState.toInt()

        if (socialTemperature == 0.0 || neighbors.isEmpty()) {
            return IntegerCellState(cellValue)
        }

        // Count occurrences of each state (including cell)
        val counts = IntArray(numStates)
        for (n in neighbors) {
            val s = n.currentState.toInt().coerceIn(0, numStates - 1)
            counts[s]++
        }
        counts[cellValue.coerceIn(0, numStates - 1)]++

        val totalCells = neighbors.size + 1

        // Compute probability of each state using Boltzmann-like weighting
        val prob = DoubleArray(numStates) { i ->
            E.pow(0.0 / socialTemperature) *
                (counts[i].toDouble() / totalCells).pow(1.0 / socialTemperature)
        }

        // Normalize (partition function)
        val z = prob.sum()
        if (z > 0) {
            for (i in prob.indices) prob[i] /= z
        }

        // Cumulative probabilities
        val cumProb = DoubleArray(numStates)
        cumProb[0] = prob[0]
        for (i in 1 until numStates) {
            cumProb[i] = cumProb[i - 1] + prob[i]
        }

        // Choose state based on random number
        val r = Random.nextDouble()
        var chosen = 0
        while (chosen < numStates - 1 && r > cumProb[chosen]) {
            chosen++
        }

        return IntegerCellState(chosen)
    }

    override fun createInitialState(): CellState = IntegerCellState(0)
}

// =============================================================================
// Symmetry (Life-like with birth {1,2,3}, survival {1,2,3})
// =============================================================================

/**
 * A Life-like rule that produces symmetric patterns from symmetric initial
 * states. Birth occurs with 1, 2, or 3 alive neighbors; survival with
 * 1, 2, or 3 alive neighbors. Uses 2-state Life extensions (dying cells
 * are not present in this simplified version).
 *
 * Port of Java Symmetry.
 */
class Symmetry : BinaryRule() {
    override val displayName = "Symmetry"
    override val description = "Produces symmetric patterns from symmetric initial states"
    override val category = RuleCategory.LIFE_LIKE
    override val compatibleLatticeNames = listOf("Square (Moore)")

    private val birthSet = setOf(1, 2, 3)
    private val survivalSet = setOf(1, 2, 3)

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val cellValue = cell.currentState.toInt()
        val aliveCount = neighbors.count { it.currentState.toInt() == 1 }

        return when {
            cellValue == 0 -> {
                if (aliveCount in birthSet) IntegerCellState(1) else IntegerCellState(0)
            }
            cellValue == 1 -> {
                if (aliveCount in survivalSet) IntegerCellState(1) else IntegerCellState(0)
            }
            else -> IntegerCellState(0)
        }
    }

    override fun createInitialState(): CellState = IntegerCellState(0)
}
