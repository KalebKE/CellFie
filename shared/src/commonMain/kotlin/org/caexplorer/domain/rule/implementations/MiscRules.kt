package org.caexplorer.domain.rule.implementations

import org.caexplorer.domain.cell.Cell
import org.caexplorer.domain.cellstate.CellState
import org.caexplorer.domain.cellstate.IntegerCellState
import org.caexplorer.domain.rule.IntegerRule
import org.caexplorer.domain.rule.Rule
import org.caexplorer.domain.rule.RuleCategory
import org.caexplorer.domain.rule.RuleProperty
import kotlin.math.pow
import kotlin.random.Random

// =============================================================================
// Random Update
// =============================================================================

/**
 * Randomly chooses a new state for each cell, independent of neighbors.
 * Produces uniform noise at every generation.
 *
 * Port of Java RandomUpdate.
 */
class RandomUpdate(override val numStates: Int = 8) : IntegerRule() {
    override val displayName = "Random Update"
    override val description = "Randomly selects arbitrary values at each time step"
    override val category = RuleCategory.PROBABILISTIC
    override val compatibleLatticeNames = listOf("Square (Moore)", "Square (Von Neumann)", "1D (radius 1)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        return IntegerCellState(Random.nextInt(numStates))
    }

    override fun createInitialState(): CellState = IntegerCellState(0)

    override val properties get() = listOf(
        RuleProperty.IntProperty("numStates", "States", numStates, 2, 256, "Number of cell states")
    )
    override fun withProperty(key: String, value: Any): Rule = when (key) {
        "numStates" -> RandomUpdate((value as Number).toInt().coerceIn(2, 256))
        else -> this
    }
}

// =============================================================================
// Lambda (Langton's λ)
// =============================================================================

/**
 * Generates a random rule table controlled by Langton's lambda parameter.
 * Lambda (0..1) is the fraction of neighborhood configurations that map
 * to a non-quiescent (non-zero) state. At lambda ≈ 0.5, behavior is near
 * the "edge of chaos."
 *
 * The rule table maps each neighborhood configuration (encoded as a base-N
 * number from the cell and its neighbors) to a new state.
 *
 * Simplified port of Java Lambda (without slider UI).
 */
class LangtonLambda(
    override val numStates: Int = 3,
    private val lambda: Double = 0.5
) : IntegerRule() {
    override val displayName = "Langton's \u03BB"
    override val description = "Rule selected by Langton's lambda parameter, near the edge of chaos"
    override val category = RuleCategory.OTHER
    override val compatibleLatticeNames = listOf("1D (radius 1)", "Square (Moore)")

    // Assume 2 neighbors for 1D; will be rebuilt on first use if different
    private var neighborCount = 2
    private var table: IntArray = buildTable(lambda, numStates, neighborCount)

    private fun buildTable(lambda: Double, numStates: Int, numNeighbors: Int): IntArray {
        val totalTransitions = numStates.toDouble().pow(numNeighbors + 1).toInt()
            .coerceAtMost(MAX_TABLE_SIZE)
        val rng = Random(42) // deterministic seed for reproducibility
        val rule = IntArray(totalTransitions)

        if (lambda < 0.5) {
            // Start all quiescent, add non-zero states
            val targetNonQuiescent = (lambda * totalTransitions).toInt()
            var added = 0
            while (added < targetNonQuiescent) {
                val i = rng.nextInt(totalTransitions)
                if (rule[i] == 0) {
                    rule[i] = 1 + rng.nextInt(numStates - 1)
                    added++
                }
            }
        } else {
            // Start all non-quiescent, add zero states
            for (i in rule.indices) {
                rule[i] = 1 + rng.nextInt(numStates - 1)
            }
            val targetQuiescent = ((1.0 - lambda) * totalTransitions).toInt()
            var added = 0
            while (added < targetQuiescent) {
                val i = rng.nextInt(totalTransitions)
                if (rule[i] != 0) {
                    rule[i] = 0
                    added++
                }
            }
        }

        return rule
    }

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        // Rebuild table if neighbor count changed
        if (neighbors.size != neighborCount) {
            neighborCount = neighbors.size
            table = buildTable(lambda, numStates, neighborCount)
        }

        val cellValue = cell.currentState.toInt().coerceIn(0, numStates - 1)

        // Build combined array: neighbors + cell (cell at end for 2D)
        val allCells = IntArray(neighbors.size + 1)
        for (i in neighbors.indices) {
            allCells[i] = neighbors[i].currentState.toInt().coerceIn(0, numStates - 1)
        }
        allCells[neighbors.size] = cellValue

        // Convert to base-10 index
        var index = 0
        for (i in allCells.indices) {
            index += allCells[i] * numStates.toDouble().pow(allCells.size - 1 - i).toInt()
        }

        index = index.coerceIn(0, table.size - 1)

        return IntegerCellState(table[index])
    }

    override fun createInitialState(): CellState = IntegerCellState(0)

    override val properties get() = listOf(
        RuleProperty.IntProperty("numStates", "States", numStates, 2, 64, "Number of cell states"),
        RuleProperty.FloatProperty("lambda", "Lambda (λ)", lambda.toFloat(), 0.0f, 1.0f, "Fraction of non-quiescent transitions")
    )
    override fun withProperty(key: String, value: Any): Rule = when (key) {
        "numStates" -> LangtonLambda((value as Number).toInt().coerceIn(2, 64), lambda)
        "lambda" -> LangtonLambda(numStates, (value as Number).toDouble().coerceIn(0.0, 1.0))
        else -> this
    }

    companion object {
        private const val MAX_TABLE_SIZE = 100000
    }
}

// =============================================================================
// Prime Death (Life-like with prime-number death)
// =============================================================================

/**
 * Life-like rule where cells are born with 2 or 6 alive neighbors, and die
 * if they have a prime number of alive neighbors. Produces many gliders.
 * Uses 3 states (alive, dying, dead) like Brian's Brain.
 *
 * Port of Java PrimeDeath.
 */
class PrimeDeath : IntegerRule() {
    override val numStates = 3
    override val displayName = "Prime Death"
    override val description = "Life-like rule with prime-number death, producing many gliders"
    override val category = RuleCategory.LIFE_LIKE
    override val compatibleLatticeNames = listOf("Square (Moore)")

    private val birthSet = setOf(2, 6)
    // Survival on non-prime numbers of alive neighbors
    private val survivalSet = setOf(4, 6, 8, 9, 10, 12, 14, 15, 16, 18, 20, 21, 22, 24, 25, 26, 27, 28)

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val cellValue = cell.currentState.toInt()
        val aliveCount = neighbors.count { it.currentState.toInt() == 1 }

        return when {
            cellValue == 0 -> {
                if (aliveCount in birthSet) IntegerCellState(1) else IntegerCellState(0)
            }
            cellValue == 1 -> {
                if (aliveCount in survivalSet) IntegerCellState(1) else IntegerCellState(2)
            }
            else -> {
                // Decay: state 2 → 0
                IntegerCellState(0)
            }
        }
    }

    override fun createInitialState(): CellState = IntegerCellState(0)
}

// =============================================================================
// Prime Plague (Life-like with prime-number survival)
// =============================================================================

/**
 * Life-like rule where cells are born with exactly 3 alive neighbors and
 * survive if they have a prime number of alive neighbors. Creates creeping
 * plague patterns. Uses 13 states for extended decay trails.
 *
 * Port of Java PrimePlague.
 */
class PrimePlague : IntegerRule() {
    override val numStates = 13
    override val displayName = "Prime Plague"
    override val description = "Life-like rule with prime-number survival, creating creeping plague"
    override val category = RuleCategory.LIFE_LIKE
    override val compatibleLatticeNames = listOf("Square (Moore)")

    private val birthSet = setOf(3)
    // Survival on prime numbers
    private val survivalSet = setOf(
        2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61,
        67, 71, 73, 79, 83, 89, 97, 101, 103, 107, 109, 113, 127, 131, 137,
        139, 149, 151, 157, 163, 167, 173
    )

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val cellValue = cell.currentState.toInt()
        val aliveCount = neighbors.count { it.currentState.toInt() == 1 }

        return when {
            cellValue == 0 -> {
                if (aliveCount in birthSet) IntegerCellState(1) else IntegerCellState(0)
            }
            cellValue == 1 -> {
                if (aliveCount in survivalSet) IntegerCellState(1) else IntegerCellState(2)
            }
            else -> {
                // Decay toward 0
                IntegerCellState((cellValue + 1) % numStates)
            }
        }
    }

    override fun createInitialState(): CellState = IntegerCellState(0)
}
