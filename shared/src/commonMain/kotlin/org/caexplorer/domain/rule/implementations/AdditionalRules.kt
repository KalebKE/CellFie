package org.caexplorer.domain.rule.implementations

import org.caexplorer.domain.cell.Cell
import org.caexplorer.domain.cellstate.CellState
import org.caexplorer.domain.cellstate.IntegerCellState
import org.caexplorer.domain.cellstate.RealValuedState
import org.caexplorer.domain.rule.BinaryRule
import org.caexplorer.domain.rule.IntegerRule
import org.caexplorer.domain.rule.RealRule
import org.caexplorer.domain.rule.RuleCategory
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.round
import kotlin.random.Random

// =============================================================================
// Langton's Ant
// =============================================================================

/**
 * Langton's Ant — a 2D Turing machine on a grid.
 *
 * The ant moves on a grid of black (1) and white (0) cells:
 * - On a white cell: turn 90° right, flip color, move forward
 * - On a black cell: turn 90° left, flip color, move forward
 *
 * Since a standard CA rule sees only local state and neighbors, we encode
 * the ant's presence and direction in the cell state:
 * - States 0-1: no ant (white=0, black=1)
 * - States 2-5: ant present, facing N/E/S/W on white
 * - States 6-9: ant present, facing N/E/S/W on black
 *
 * The rule checks if a neighbor's ant is about to move into this cell.
 *
 * Port inspired by Java TuringMachine concepts.
 */
class LangtonsAnt : IntegerRule() {
    override val numStates = 10
    override val displayName = "Langton's Ant"
    override val description = "Turing-complete ant that produces emergent highways"
    override val category = RuleCategory.OTHER
    override val compatibleLatticeNames = listOf("Square (Von Neumann)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val state = cell.currentState.toInt()

        // Check if an ant from a neighbor is moving into this cell
        // Neighbors are expected in order: N, E, S, W
        if (neighbors.size >= 4) {
            for (i in 0 until 4) {
                val nState = neighbors[i].currentState.toInt()
                if (nState >= 2) {
                    // Neighbor has an ant
                    val antOnBlack = nState >= 6
                    val dir = if (antOnBlack) nState - 6 else nState - 2
                    // Determine ant's new direction after turning
                    val newDir = if (antOnBlack) {
                        (dir + 3) % 4 // Turn left
                    } else {
                        (dir + 1) % 4 // Turn right
                    }
                    // The ant moves in newDir direction. Check if it lands here.
                    // Neighbor i is in direction i from us, so ant coming FROM i
                    // means the ant was moving in direction opposite to i.
                    // Neighbor 0=N means that neighbor is to our North.
                    // For ant to arrive here from neighbor i, the ant must be
                    // moving South (dir=2) from neighbor 0 (North of us).
                    val targetDir = (i + 2) % 4 // Direction from neighbor to us
                    if (newDir == targetDir) {
                        // Ant arrives here! Encode ant with current cell's ground color
                        val groundColor = state % 2 // 0=white, 1=black (ignore if ant was here)
                        return if (state >= 2) {
                            // There was already an ant here (shouldn't normally happen)
                            IntegerCellState(state)
                        } else {
                            // Place ant with direction newDir on ground color
                            IntegerCellState(if (groundColor == 1) 6 + newDir else 2 + newDir)
                        }
                    }
                }
            }
        }

        // If this cell currently has an ant, the ant leaves: flip the cell color
        if (state >= 2) {
            val wasOnBlack = state >= 6
            return IntegerCellState(if (wasOnBlack) 0 else 1)
        }

        // No ant arrives and no ant here — cell stays the same
        return IntegerCellState(state)
    }

    override fun createInitialState(): CellState = IntegerCellState(0)

    companion object {
        const val DIR_N = 0; const val DIR_E = 1; const val DIR_S = 2; const val DIR_W = 3
    }
}

// =============================================================================
// Continuous CA (real-valued Wolfram-like)
// =============================================================================

/**
 * Continuous CA: Averages the cell and its neighbors, applies a linear
 * function y = slope * avg + yIntercept, then keeps only the fractional part.
 *
 * Produces Wolfram-like patterns with real numbers. Different slope and
 * y-intercept values produce all four Wolfram classes.
 *
 * Port of Java ContinuousCA.
 */
class ContinuousCA(
    val slope: Double = 1.0,
    val yIntercept: Double = 0.1
) : RealRule() {
    override val displayName = "Continuous CA"
    override val description = "Real-valued Wolfram-like rule (slope=$slope, b=$yIntercept)"
    override val category = RuleCategory.CONTINUOUS
    override val compatibleLatticeNames = listOf("Square (Moore)", "Square (Von Neumann)", "1D (radius 1)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val cellVal = (cell.currentState as RealValuedState).state

        // Average the cell and all its neighbors
        var avg = cellVal
        for (n in neighbors) {
            avg += (n.currentState as RealValuedState).state
        }
        avg /= (neighbors.size + 1).toDouble()

        // Apply linear function and keep fractional part
        var result = slope * avg + yIntercept
        result -= floor(result)

        return RealValuedState(result)
    }

    override fun createInitialState(): CellState = RealValuedState(0.0)

    companion object {
        val DEFAULT = ContinuousCA(1.0, 0.1)
        val CLASS_IV = ContinuousCA(1.0, 0.408)
        val CHAOTIC = ContinuousCA(-1.5, 0.76)
    }
}

// =============================================================================
// Real Spirals
// =============================================================================

/**
 * Real Spirals: Cells always increase in value, but increase by double
 * the normal amount when surrounded by 2+ neighbors with larger values.
 * Produces beautiful spiral patterns similar to cyclic CA but with
 * continuous values.
 *
 * Port of Java RealSpirals.
 */
class RealSpirals(val maxValue: Double = 1000.0) : RealRule() {
    override val displayName = "Real Spirals"
    override val description = "Continuous spiral patterns from real-valued cyclic growth"
    override val category = RuleCategory.CONTINUOUS
    override val compatibleLatticeNames = listOf("Square (Moore)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val cellVal = (cell.currentState as RealValuedState).state
        val threshold = 20.0
        val triggerCount = 2

        // Count neighbors with significantly larger values
        var count = 0
        for (n in neighbors) {
            if ((n.currentState as RealValuedState).state > cellVal + threshold) {
                count++
            }
        }

        // Increase faster if triggered
        var result = if (count > triggerCount) {
            cellVal + threshold
        } else {
            cellVal + threshold / 2.0
        }

        // Wrap around via modulo
        if (result > maxValue) {
            result = (result / maxValue - floor(result / maxValue)) * maxValue
        }

        return RealValuedState(result, 0.0, maxValue)
    }

    override fun createInitialState(): CellState = RealValuedState(0.0, 0.0, 1000.0)
}

// =============================================================================
// Spirals (Life-like extension with slow death)
// =============================================================================

/**
 * Spirals: A multi-state Life-like rule producing unwinding spirals.
 *
 * - A dead cell (state 0) is born to state 1 if it has 2, 3, or 4
 *   neighbors in state 1.
 * - A live cell (state 1) survives if it has exactly 2 neighbors in state 1.
 * - If a cell fails to survive, it begins dying: state increments each
 *   generation until it wraps back to 0.
 *
 * Port of Java Spirals rule.
 */
class Spirals(override val numStates: Int = 5) : IntegerRule() {
    override val displayName = "Spirals"
    override val description = "Unwinding spirals from a Life-like rule with slow death"
    override val category = RuleCategory.LIFE_LIKE
    override val compatibleLatticeNames = listOf("Square (Moore)")

    private val birthValues = intArrayOf(2, 3, 4)
    private val survivalValues = intArrayOf(2)

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val cellValue = cell.currentState.toInt()

        // Count neighbors in state 1
        var onesCount = 0
        for (n in neighbors) {
            if (n.currentState.toInt() == 1) onesCount++
        }

        val nextVal = when {
            // Dead cell — check for birth
            cellValue == 0 -> {
                if (onesCount in birthValues) 1 else 0
            }
            // Alive cell — check for survival
            cellValue == 1 -> {
                if (onesCount in survivalValues) 1
                else (cellValue + 1) % numStates // Begin dying
            }
            // Dying cell — continue death process
            else -> (cellValue + 1) % numStates
        }

        return IntegerCellState(nextVal)
    }

    override fun createInitialState(): CellState = IntegerCellState(0)

    private operator fun IntArray.contains(value: Int): Boolean {
        for (v in this) if (v == value) return true
        return false
    }
}

// =============================================================================
// Majority Wins (Voting rule)
// =============================================================================

/**
 * Majority Wins: Each cell adopts the state held by the majority of its
 * neighbors. Ties are broken randomly.
 *
 * Models opinion dynamics / voting behavior. With 2 states and 50%
 * random initial conditions, domains emerge and slowly merge.
 *
 * Port of Java MajorityWins.
 */
class MajorityVote(
    override val numStates: Int = 2,
    private val includeSelf: Boolean = true
) : IntegerRule() {
    override val displayName = if (includeSelf) "Majority Vote" else "Majority Vote (no self)"
    override val description = "Cell adopts the most common state among neighbors"
    override val category = RuleCategory.SOCIAL
    override val compatibleLatticeNames = listOf("Square (Moore)", "Square (Von Neumann)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val counts = IntArray(numStates)

        // Count neighbors
        for (n in neighbors) {
            val s = n.currentState.toInt()
            if (s in counts.indices) counts[s]++
        }

        // Optionally include self
        if (includeSelf) {
            val s = cell.currentState.toInt()
            if (s in counts.indices) counts[s]++
        }

        // Find maximum count and collect tied states
        var maxCount = 0
        var maxState = 0
        var tieCount = 0

        for (i in counts.indices) {
            if (counts[i] > maxCount) {
                maxCount = counts[i]
                maxState = i
                tieCount = 1
            } else if (counts[i] == maxCount) {
                tieCount++
            }
        }

        // If tied, pick randomly among the tied states
        if (tieCount > 1) {
            val targetTie = Random.nextInt(tieCount)
            var tieIndex = 0
            for (i in counts.indices) {
                if (counts[i] == maxCount) {
                    if (tieIndex == targetTie) {
                        maxState = i
                        break
                    }
                    tieIndex++
                }
            }
        }

        return IntegerCellState(maxState)
    }

    override fun createInitialState(): CellState = IntegerCellState(0)
}

// =============================================================================
// Neural Net CA
// =============================================================================

/**
 * Neural Net: Each cell is a neuron. The neighbors serve as inputs with
 * uniform weights. A sigmoid transfer function produces the output, which
 * is scaled and rounded to the nearest integer state.
 *
 * Produces domain-like patterns that self-organize.
 *
 * Port of Java NeuralNet.
 */
class NeuralNetCA(override val numStates: Int = 5) : IntegerRule() {
    override val displayName = "Neural Net"
    override val description = "Each cell is a neuron with sigmoid activation"
    override val category = RuleCategory.NEURAL
    override val compatibleLatticeNames = listOf("Square (Moore)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val halfRange = (numStates - 1.0) / 2.0

        // The slope is empirically tuned to keep behavior near criticality
        val slope = 1.0 / ((neighbors.size / 2.0) * numStates.toDouble())

        // Sum weighted inputs (rescaled to center around 0)
        var weightedSum = 0.0
        for (n in neighbors) {
            val rescaled = n.currentState.toInt().toDouble() - halfRange
            weightedSum += rescaled // weight = 1.0
        }

        // Sigmoid transfer function: output in (0, 1)
        val sigmoidOutput = 1.0 / (1.0 + exp(-slope * weightedSum))

        // Scale to [0, numStates-1] and round
        val scaled = sigmoidOutput * (numStates - 1)
        val cellValue = round(scaled).toInt().coerceIn(0, numStates - 1)

        return IntegerCellState(cellValue)
    }

    override fun createInitialState(): CellState = IntegerCellState(0)
}

// =============================================================================
// Growing Seed
// =============================================================================

/**
 * Growing Seed: If any neighbor is non-zero, a zero cell becomes 1.
 * Non-zero cells increment their state each generation (modulo numStates).
 * Produces colorful expanding waves from a seed.
 *
 * Port of Java GrowingSeed.
 */
class GrowingSeed(override val numStates: Int = 16) : IntegerRule() {
    override val displayName = "Growing Seed"
    override val description = "Cyclic growth that sweeps outward from a seed"
    override val category = RuleCategory.OTHER
    override val compatibleLatticeNames = listOf("Square (Moore)", "Square (Von Neumann)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val cellValue = cell.currentState.toInt()

        if (cellValue > 0) {
            // Increment non-zero cells
            return IntegerCellState((cellValue + 1) % numStates)
        }

        // Cell is 0: become 1 if any neighbor is non-zero
        for (n in neighbors) {
            if (n.currentState.toInt() != 0) {
                return IntegerCellState(1)
            }
        }

        return IntegerCellState(0)
    }

    override fun createInitialState(): CellState = IntegerCellState(0)
}

// =============================================================================
// Lava Lamp (Cyclic CA with high trigger)
// =============================================================================

/**
 * Lava Lamp: A cyclic CA where a cell advances from state i to i+1 only
 * when at least 10 neighbors are already in state i+1. Requires an
 * extended neighborhood (24 neighbors) to produce lava-lamp-like blobs.
 *
 * Port of Java LavaLamp.
 */
class LavaLamp(override val numStates: Int = 3, val triggerNumber: Int = 10) : IntegerRule() {
    override val displayName = "Lava Lamp"
    override val description = "Lava-like bubbles from a high-threshold cyclic CA"
    override val category = RuleCategory.OTHER
    override val compatibleLatticeNames = listOf("Square (Moore)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val state = cell.currentState.toInt()
        val nextState = (state + 1) % numStates

        // Count neighbors in the next cyclic state
        var count = 0
        for (n in neighbors) {
            if (n.currentState.toInt() == nextState) count++
        }

        return if (count >= triggerNumber) {
            IntegerCellState(nextState)
        } else {
            IntegerCellState(state)
        }
    }

    override fun createInitialState(): CellState = IntegerCellState(0)
}

// =============================================================================
// Additional Life-like rules with interesting behavior
// =============================================================================

/**
 * Anneal (B4678/S35678): A Life-like rule that tends to produce large
 * stable islands. Also known as the "twisted majority" rule.
 */
class Anneal : BinaryRule() {
    override val displayName = "Anneal"
    override val description = "Life-like rule B4678/S35678 — forms stable islands"
    override val category = RuleCategory.LIFE_LIKE
    override val compatibleLatticeNames = listOf("Square (Moore)")

    private val birth = intArrayOf(4, 6, 7, 8)
    private val survival = intArrayOf(3, 5, 6, 7, 8)

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

    private operator fun IntArray.contains(value: Int): Boolean {
        for (v in this) if (v == value) return true
        return false
    }
}

/**
 * Maze rule (B3/S12345): Generates complex maze-like structures
 * from a random initial state.
 */
class MazeRule : BinaryRule() {
    override val displayName = "Maze"
    override val description = "Life-like rule B3/S12345 — generates maze patterns"
    override val category = RuleCategory.LIFE_LIKE
    override val compatibleLatticeNames = listOf("Square (Moore)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val alive = cell.currentState.toInt() != 0
        var liveNeighbors = 0
        for (n in neighbors) {
            if (n.currentState.toInt() != 0) liveNeighbors++
        }

        val nextAlive = if (alive) {
            liveNeighbors in 1..5
        } else {
            liveNeighbors == 3
        }

        return IntegerCellState(if (nextAlive) 1 else 0)
    }

    override fun createInitialState(): CellState = IntegerCellState(0)
}
