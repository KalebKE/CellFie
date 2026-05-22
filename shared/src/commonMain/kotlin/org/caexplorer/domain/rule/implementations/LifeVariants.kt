package org.caexplorer.domain.rule.implementations

import org.caexplorer.domain.cell.Cell
import org.caexplorer.domain.cellstate.CellState
import org.caexplorer.domain.cellstate.IntegerCellState
import org.caexplorer.domain.cellstate.RealValuedState
import org.caexplorer.domain.rule.IntegerRule
import org.caexplorer.domain.rule.RealRule
import org.caexplorer.domain.rule.Rule
import org.caexplorer.domain.rule.RuleCategory
import org.caexplorer.domain.rule.RuleProperty
import kotlin.math.floor
import kotlin.math.pow
import kotlin.random.Random

// =============================================================================
// Bunnies (Complex neighbor-index-based rule)
// =============================================================================

/**
 * Complex Life-like rule with 73 states that creates bunny-shaped patterns.
 * Cells are born based on specific neighbor index patterns. States > 1
 * decay toward 0 (like Brian's Brain extended).
 *
 * Birth occurs when specific combinations of neighbor positions have state 1.
 * The original Java version checks individual neighbor positions for state 1.
 *
 * Port of Java Bunnies.
 */
class Bunnies : IntegerRule() {
    override val numStates = 73
    override val displayName = "Bunnies"
    override val description = "Complex Life-like rule producing bunny-shaped gliders with 73-state decay"
    override val category = RuleCategory.LIFE_LIKE
    override val compatibleLatticeNames = listOf("Square (Moore)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val cellValue = cell.currentState.toInt()

        if (neighbors.size < 8) {
            return IntegerCellState(cellValue)
        }

        // Count alive (state 1) neighbors
        val aliveCount = neighbors.count { it.currentState.toInt() == 1 }

        return when {
            // Dead cell: birth conditions
            cellValue == 0 -> {
                // Birth if specific neighbor index patterns match
                // Original checks individual neighbor positions for bunny shapes
                val n = IntArray(8) { i -> if (neighbors[i].currentState.toInt() == 1) 1 else 0 }

                val birth = (n[0] == 1 && n[1] == 1 && aliveCount == 2) ||
                    (n[2] == 1 && n[5] == 1 && aliveCount == 2) ||
                    (n[3] == 1 && n[4] == 1 && aliveCount == 2) ||
                    (n[6] == 1 && n[7] == 1 && aliveCount == 2) ||
                    (aliveCount == 3 && n[0] == 1 && n[4] == 1 && n[7] == 1) ||
                    (aliveCount == 3 && n[1] == 1 && n[5] == 1 && n[6] == 1) ||
                    (aliveCount == 3 && n[2] == 1 && n[3] == 1 && n[7] == 1) ||
                    (aliveCount == 4 && n[0] == 1 && n[2] == 1 && n[5] == 1 && n[7] == 1)

                if (birth) IntegerCellState(1) else IntegerCellState(0)
            }
            // Alive cell: survival conditions
            cellValue == 1 -> {
                if (aliveCount in 2..3) {
                    IntegerCellState(1)
                } else {
                    IntegerCellState(2) // Begin dying
                }
            }
            // Dying cell: decay
            else -> {
                IntegerCellState((cellValue + 1) % numStates)
            }
        }
    }

    override fun createInitialState(): CellState = IntegerCellState(0)
}

// =============================================================================
// Crystal Life (Outer Totalistic 435 with running average)
// =============================================================================

/**
 * Outer totalistic rule 435 with 2 states and a running average of depth 2.
 * The running average smooths the rule output by averaging with the
 * previous state.
 *
 * Port of Java CrystalLife.
 */
class CrystalLife : OuterTotalisticRule(
    ruleNumber = 435,
    numStates = 2,
    displayName = "Crystal Life",
    description = "Crystalline life patterns using outer totalistic rule 435 with running average",
    category = RuleCategory.LIFE_LIKE,
    compatibleLatticeNames = listOf("Square (Moore)"),
    runningAverageDepth = 2
)

// =============================================================================
// Drunk Gliders (Spirals variant for hexagonal lattice)
// =============================================================================

/**
 * Spirals-like rule with 5 states, designed for hexagonal lattices.
 * A cell advances to the next cyclic state if at least 1 neighbor is in
 * the successor state.
 *
 * Port of Java DrunkGliders.
 */
class DrunkGliders : IntegerRule() {
    override val numStates = 5
    override val displayName = "Drunk Gliders"
    override val description = "Spirals-like cyclic rule producing irregular gliders on hexagonal lattice"
    override val category = RuleCategory.TOTALISTIC
    override val compatibleLatticeNames = listOf("Hexagonal")

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

// =============================================================================
// Tunnelling Spaceships
// =============================================================================

/**
 * Concatenates neighbor values as digits of a base-N number, takes the
 * result modulo the number of neighbors, and uses that index to select
 * the new cell value from the neighbor array.
 *
 * Port of Java TunnellingSpaceships.
 */
class TunnellingSpaceships(override val numStates: Int = 5) : IntegerRule() {
    override val displayName = "Tunnelling Spaceships"
    override val description = "Neighbor values form a base-N number; modular index selects new state"
    override val category = RuleCategory.OTHER
    override val compatibleLatticeNames = listOf("Square (Moore)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        if (neighbors.isEmpty()) return IntegerCellState(cell.currentState.toInt())

        // Concatenate neighbor values as base-N number
        var baseNValue = 0L
        for (n in neighbors) {
            baseNValue = baseNValue * numStates + n.currentState.toInt().coerceIn(0, numStates - 1)
        }

        // Take modulo number of neighbors
        val index = (baseNValue % neighbors.size).toInt()

        return IntegerCellState(neighbors[index].currentState.toInt().coerceIn(0, numStates - 1))
    }

    override fun createInitialState(): CellState = IntegerCellState(0)

    override val properties get() = listOf(
        RuleProperty.IntProperty("numStates", "States", numStates, 2, 256, "Number of cell states")
    )
    override fun withProperty(key: String, value: Any): Rule = when (key) {
        "numStates" -> TunnellingSpaceships((value as Number).toInt().coerceIn(2, 256))
        else -> this
    }
}

// =============================================================================
// Water Skimmers (Real-valued with noise)
// =============================================================================

/**
 * Real-valued rule that averages cell and neighbor values, then adds a small
 * random perturbation (random * 10). Result is taken modulo 1000.
 *
 * Port of Java WaterSkimmers.
 */
class WaterSkimmers : RealRule() {
    override val displayName = "Water Skimmers"
    override val description = "Real-valued averaging with random perturbation producing skimmer patterns"
    override val category = RuleCategory.CONTINUOUS
    override val compatibleLatticeNames = listOf("Square (Moore)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val cellVal = (cell.currentState as? RealValuedState)?.state ?: cell.currentState.toInt().toDouble()

        var sum = cellVal
        for (n in neighbors) {
            sum += (n.currentState as? RealValuedState)?.state ?: n.currentState.toInt().toDouble()
        }
        val avg = sum / (neighbors.size + 1)

        var result = avg + Random.nextDouble() * 10.0

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
