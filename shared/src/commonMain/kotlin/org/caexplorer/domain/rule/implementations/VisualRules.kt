package org.caexplorer.domain.rule.implementations

import org.caexplorer.domain.cell.Cell
import org.caexplorer.domain.cellstate.CellState
import org.caexplorer.domain.cellstate.IntegerCellState
import org.caexplorer.domain.cellstate.RealValuedState
import org.caexplorer.domain.rule.IntegerRule
import org.caexplorer.domain.rule.RealRule
import org.caexplorer.domain.rule.RuleCategory
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin

// =============================================================================
// Snowflake (Outer Totalistic Rule 846)
// =============================================================================

/**
 * Snowflake growth pattern using outer totalistic rule 846 with 2 states.
 *
 * Port of Java Snowflake.
 */
class Snowflake : OuterTotalisticRule(
    ruleNumber = 846,
    numStates = 2,
    displayName = "Snowflake",
    description = "Snowflake growth patterns using outer totalistic rule 846",
    category = RuleCategory.TOTALISTIC,
    compatibleLatticeNames = listOf("Square (Moore)")
)

// =============================================================================
// Snowflake Dust (Outer Totalistic Rule 68)
// =============================================================================

/**
 * Produces scattered snowflake dust patterns using outer totalistic rule 68.
 *
 * Port of Java SnowflakeDust.
 */
class SnowflakeDust : OuterTotalisticRule(
    ruleNumber = 68,
    numStates = 2,
    displayName = "Snowflake Dust",
    description = "Scattered snowflake dust using outer totalistic rule 68",
    category = RuleCategory.TOTALISTIC,
    compatibleLatticeNames = listOf("Square (Moore)")
)

// =============================================================================
// Snowflake Maze (Outer Totalistic Rule 750)
// =============================================================================

/**
 * Creates maze-like snowflake patterns using outer totalistic rule 750.
 *
 * Port of Java SnowflakeMaze.
 */
class SnowflakeMaze : OuterTotalisticRule(
    ruleNumber = 750,
    numStates = 2,
    displayName = "Snowflake Maze",
    description = "Maze-like snowflake patterns using outer totalistic rule 750",
    category = RuleCategory.TOTALISTIC,
    compatibleLatticeNames = listOf("Square (Moore)")
)

// =============================================================================
// Pulsing Snowflake (Q2R-like with checkerboard)
// =============================================================================

/**
 * A Q2R-like rule with 3 states that creates pulsing snowflake patterns.
 * Uses a checkerboard pattern based on cell coordinates and an internal
 * generation counter. On "even" cells, flips between 0 and 1 based on
 * neighbor majority; on "odd" cells, flips between 1 and 2.
 *
 * Port of Java PulsingSnowflake.
 */
class PulsingSnowflake : IntegerRule() {
    override val numStates = 3
    override val displayName = "Pulsing Snowflake"
    override val description = "Q2R-like pulsing snowflake with checkerboard dynamics"
    override val category = RuleCategory.PHYSICS
    override val compatibleLatticeNames = listOf("Square (Von Neumann)")

    @Volatile
    private var generation = 0

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val cellValue = cell.currentState.toInt()
        val row = cell.coordinate.row
        val col = cell.coordinate.col

        // Update generation counter based on cell generation
        generation = cell.generation

        // Checkerboard: even/odd based on (row + col + generation) parity
        val isEvenPhase = (row + col + generation) % 2 == 0

        val totalSpin = neighbors.sumOf { it.currentState.toInt() } + cellValue
        val totalCells = neighbors.size + 1

        return if (isEvenPhase) {
            // Even phase: update based on local majority
            val majority = if (totalSpin * 2 > totalCells * (numStates - 1)) {
                (cellValue + 1).coerceAtMost(numStates - 1)
            } else if (totalSpin * 2 < totalCells) {
                (cellValue - 1).coerceAtLeast(0)
            } else {
                cellValue
            }
            IntegerCellState(majority)
        } else {
            // Odd phase: keep current state (checkerboard skip)
            IntegerCellState(cellValue)
        }
    }

    override fun createInitialState(): CellState = IntegerCellState(0)
}

// =============================================================================
// Lightning (1D Totalistic Rule 6055785317397)
// =============================================================================

/**
 * A 1D totalistic rule that produces lightning-like branching patterns.
 * Uses rule number 6055785317397 with 3 states on a 1D lattice.
 *
 * The rule number encodes a totalistic lookup table: the sum of the cell
 * and its two neighbors (range 0..6 for 3 states, 2 neighbors) maps to a
 * new state.
 *
 * Port of Java Lightning.
 */
class Lightning : IntegerRule() {
    override val numStates = 3
    override val displayName = "Lightning"
    override val description = "Lightning-like branching on a 1D lattice with 3 states"
    override val category = RuleCategory.TOTALISTIC
    override val compatibleLatticeNames = listOf("1D (radius 1)")

    // For 3 states, 2 neighbors: each cell+neighbors ranges 0..6
    // Totalistic: lookup[sum] where sum = cell + left + right
    // But the Java "RuleNumber" is different - it's a full neighborhood lookup.
    // For 1D with 2 neighbors and 3 states: 3^3 = 27 transitions.
    // Rule number 6055785317397 in base 3 gives 27 digits.
    private val table: IntArray

    init {
        val numTransitions = numStates.toDouble().pow(numStates).toInt() // 3^3 = 27
        val digits = OuterTotalisticRule.convertToBaseN(6055785317397L, numStates, numTransitions)
        table = digits
    }

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val cellValue = cell.currentState.toInt().coerceIn(0, numStates - 1)

        // Build index from neighborhood configuration (like Wolfram rule)
        // For 1D: left, cell, right → index = left * 9 + cell * 3 + right
        val left = if (neighbors.isNotEmpty()) neighbors[0].currentState.toInt().coerceIn(0, numStates - 1) else 0
        val right = if (neighbors.size > 1) neighbors[neighbors.size - 1].currentState.toInt().coerceIn(0, numStates - 1) else 0

        val index = (left * numStates * numStates + cellValue * numStates + right)
            .coerceIn(0, table.size - 1)

        return IntegerCellState(table[index])
    }

    override fun createInitialState(): CellState = IntegerCellState(0)
}

// =============================================================================
// Thunderstorm (Real-valued with sine wave)
// =============================================================================

/**
 * Real-valued rule that averages cell and neighbor values, then adds a
 * time-varying sine wave component: 25 * (1 + sin(generation / 10)).
 * Result is taken modulo 1000.
 *
 * Port of Java ThunderStorm.
 */
class ThunderStorm : RealRule() {
    override val displayName = "Thunderstorm"
    override val description = "Real-valued averaging with periodic sine wave fluctuations"
    override val category = RuleCategory.CONTINUOUS
    override val compatibleLatticeNames = listOf("Square (Moore)")

    @Volatile
    private var generation = 0

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        generation = cell.generation
        val cellVal = (cell.currentState as? RealValuedState)?.state ?: cell.currentState.toInt().toDouble()

        var sum = cellVal
        for (n in neighbors) {
            sum += (n.currentState as? RealValuedState)?.state ?: n.currentState.toInt().toDouble()
        }
        val avg = sum / (neighbors.size + 1)

        var result = avg + 25.0 * (1.0 + sin(generation / 10.0))

        // Modulo 1000
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
// Fireworks (Life Extensions with even/odd neighbor rules)
// =============================================================================

/**
 * Life-like rule with 50 states where cells are born when they have an even
 * number of alive neighbors, and survive when they have an odd number.
 * States > 1 decay toward 0 (like Brian's Brain extended).
 *
 * Port of Java Fireworks.
 */
class Fireworks : IntegerRule() {
    override val numStates = 50
    override val displayName = "Fireworks"
    override val description = "Birth on even neighbor counts, survival on odd, with 50-state decay"
    override val category = RuleCategory.LIFE_LIKE
    override val compatibleLatticeNames = listOf("Square (Moore)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val cellValue = cell.currentState.toInt()

        // Count neighbors with state == 1 (alive)
        val aliveCount = neighbors.count { it.currentState.toInt() == 1 }

        return when {
            // Cell is dead (state 0): birth if even number of alive neighbors > 0
            cellValue == 0 -> {
                if (aliveCount > 0 && aliveCount % 2 == 0) {
                    IntegerCellState(1)
                } else {
                    IntegerCellState(0)
                }
            }
            // Cell is alive (state 1): survive if odd number of alive neighbors
            cellValue == 1 -> {
                if (aliveCount % 2 == 1) {
                    IntegerCellState(1)
                } else {
                    IntegerCellState(2) // Begin dying
                }
            }
            // Cell is dying (states 2..numStates-1): decay toward 0
            else -> {
                IntegerCellState((cellValue + 1) % numStates)
            }
        }
    }

    override fun createInitialState(): CellState = IntegerCellState(0)
}

// =============================================================================
// Colliding Cyclones (Cyclic CA with trigger=3, 3 states)
// =============================================================================

/**
 * Cyclic CA variant with 3 states and trigger threshold of 3.
 * A cell advances to the next state if it has at least 3 neighbors
 * in the successor state.
 *
 * Port of Java CollidingCyclones.
 */
class CollidingCyclones : IntegerRule() {
    override val numStates = 3
    override val displayName = "Colliding Cyclones"
    override val description = "Cyclic CA with 3 states and trigger threshold of 3"
    override val category = RuleCategory.TOTALISTIC
    override val compatibleLatticeNames = listOf("Square (Moore)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val cellValue = cell.currentState.toInt()
        val nextValue = (cellValue + 1) % numStates

        val count = neighbors.count { it.currentState.toInt() == nextValue }

        return if (count >= 3) {
            IntegerCellState(nextValue)
        } else {
            IntegerCellState(cellValue)
        }
    }

    override fun createInitialState(): CellState = IntegerCellState(0)
}

// =============================================================================
// Epileptic Blobs (Outer Totalistic Rule 429)
// =============================================================================

/**
 * Creates epileptic blob-like patterns using outer totalistic rule 429.
 *
 * Port of Java EpilepticBlobs.
 */
class EpilepticBlobs : OuterTotalisticRule(
    ruleNumber = 429,
    numStates = 2,
    displayName = "Epileptic Blobs",
    description = "Epileptic blob patterns using outer totalistic rule 429",
    category = RuleCategory.TOTALISTIC,
    compatibleLatticeNames = listOf("Square (Moore)")
)
