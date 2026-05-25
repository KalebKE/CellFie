package org.caexplorer.domain.rule.implementations

import org.caexplorer.domain.cell.Cell
import org.caexplorer.domain.cellstate.CellState
import org.caexplorer.domain.cellstate.IntegerCellState
import org.caexplorer.domain.cellstate.RealValuedState
import org.caexplorer.domain.rule.BinaryRule
import org.caexplorer.domain.rule.IntegerRule
import org.caexplorer.domain.rule.RealRule
import org.caexplorer.domain.rule.Rule
import org.caexplorer.domain.rule.RuleCategory
import org.caexplorer.domain.rule.RuleProperty
import kotlin.random.Random

/**
 * Cyclic Cellular Automaton.
 * Each cell advances to the next state if at least one neighbor is in the next state.
 */
class CyclicCA(override val numStates: Int = 14, val threshold: Int = 1) : IntegerRule() {
    override val displayName = "Cyclic CA"
    override val description = "Cyclic cellular automaton with $numStates states"
    override val category = RuleCategory.OTHER
    override val compatibleLatticeNames = listOf("Square (Moore)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val state = cell.currentState.toInt()
        val nextState = (state + 1) % numStates
        var count = 0
        for (n in neighbors) {
            if (n.currentState.toInt() == nextState) count++
        }
        return if (count >= threshold) IntegerCellState(nextState) else IntegerCellState(state)
    }

    override fun createInitialState(): CellState = IntegerCellState(0)

    override val properties get() = listOf(
        RuleProperty.IntProperty("numStates", "States", numStates, 2, 256, "Number of cell states"),
        RuleProperty.IntProperty("threshold", "Threshold", threshold, 1, 8, "Neighbors needed to advance")
    )
    override fun withProperty(key: String, value: Any): Rule = when (key) {
        "numStates" -> CyclicCA((value as Number).toInt().coerceIn(2, 256), threshold)
        "threshold" -> CyclicCA(numStates, (value as Number).toInt().coerceIn(1, 8))
        else -> this
    }
}

/**
 * Forest Fire simulation with 8 growth stages.
 * States: 0 = bare ground, 1-4 = growth stages, 5 = burning, 6 = smoldering, 7 = ashes
 */
class ForestFire(
    val growthProbability: Double = 0.01,
    val lightningProbability: Double = 0.0001
) : IntegerRule() {
    override val numStates = 8
    override val displayName = "Forest Fire"
    override val description = "Forest fire model with growth stages"
    override val category = RuleCategory.PROBABILISTIC
    override val compatibleLatticeNames = listOf("Square (Moore)", "Square (Von Neumann)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val state = cell.currentState.toInt()
        val nextVal = when (state) {
            BURNING -> SMOLDERING
            SMOLDERING -> ASHES
            ASHES -> BARE_GROUND
            BARE_GROUND -> {
                if (Random.nextDouble() < growthProbability) SEEDLING else BARE_GROUND
            }
            SEEDLING, SAPLING, YOUNG_TREE, MATURE_TREE -> {
                val hasFireNeighbor = neighbors.any { it.currentState.toInt() == BURNING || it.currentState.toInt() == SMOLDERING }
                if (hasFireNeighbor) BURNING
                else if (state == MATURE_TREE && Random.nextDouble() < lightningProbability) BURNING
                else when (state) {
                    SEEDLING -> SAPLING
                    SAPLING -> YOUNG_TREE
                    YOUNG_TREE -> MATURE_TREE
                    else -> MATURE_TREE
                }
            }
            else -> BARE_GROUND
        }
        return IntegerCellState(nextVal)
    }

    override fun createInitialState(): CellState = IntegerCellState(0)

    companion object {
        const val BARE_GROUND = 0
        const val SEEDLING = 1
        const val SAPLING = 2
        const val YOUNG_TREE = 3
        const val MATURE_TREE = 4
        const val BURNING = 5
        const val SMOLDERING = 6
        const val ASHES = 7
    }
}

/**
 * Diffusion simulation on a real-valued grid.
 */
class Diffusion(val diffusionRate: Double = 0.1) : RealRule() {
    override val displayName = "Diffusion"
    override val description = "Diffusion model with rate=$diffusionRate"
    override val category = RuleCategory.PHYSICS
    override val compatibleLatticeNames = listOf("Square (Moore)", "Square (Von Neumann)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val current = (cell.currentState as RealValuedState).state
        var neighborSum = 0.0
        for (n in neighbors) {
            neighborSum += (n.currentState as RealValuedState).state
        }
        val neighborAvg = neighborSum / neighbors.size
        val next = current + diffusionRate * (neighborAvg - current)
        return RealValuedState(next)
    }

    override fun createInitialState(): CellState = RealValuedState(0.0)
}

/**
 * Diffusion-Limited Aggregation.
 */
class DiffusionLimitedAggregation : BinaryRule() {
    override val displayName = "DLA"
    override val description = "Diffusion-limited aggregation"
    override val category = RuleCategory.FRACTAL
    override val compatibleLatticeNames = listOf("Square (Moore)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val state = cell.currentState.toInt()
        if (state == 1) return IntegerCellState(1)
        val hasAggregateNeighbor = neighbors.any { it.currentState.toInt() == 1 }
        return if (hasAggregateNeighbor && Random.nextDouble() < 0.3) {
            IntegerCellState(1)
        } else IntegerCellState(0)
    }

    override fun createInitialState(): CellState = IntegerCellState(0)
}

/**
 * Ising model from statistical physics.
 */
class IsingModel(
    val temperature: Double = 2.27,
    val magneticField: Double = 0.0,
    val exchangeJ: Double = 1.0
) : BinaryRule() {
    override val displayName = "Ising Model"
    override val description = "Ising model at T=${"%.2f".format(temperature)}"
    override val category = RuleCategory.PHYSICS
    override val compatibleLatticeNames = listOf("Square (Moore)", "Square (Von Neumann)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val spin = if (cell.currentState.toInt() == 1) 1 else -1
        var neighborSum = 0
        for (n in neighbors) {
            neighborSum += if (n.currentState.toInt() == 1) 1 else -1
        }
        val deltaE = 2.0 * spin * (magneticField + exchangeJ * neighborSum)
        val flip = if (deltaE <= 0) true
        else Random.nextDouble() < kotlin.math.exp(-deltaE / temperature)
        val newSpin = if (flip) -spin else spin
        return IntegerCellState(if (newSpin == 1) 1 else 0)
    }

    override fun createInitialState(): CellState = IntegerCellState(0)

    override val properties get() = listOf(
        RuleProperty.FloatProperty("temperature", "Temperature", temperature.toFloat(), 0.1f, 10.0f, "Thermal energy (Tc ≈ 2.27)"),
        RuleProperty.FloatProperty("magneticField", "Field H", magneticField.toFloat(), -2.0f, 2.0f, "External magnetic field"),
        RuleProperty.FloatProperty("exchangeJ", "Exchange J", exchangeJ.toFloat(), -2.0f, 2.0f, "Spin-spin coupling strength")
    )
    override fun withProperty(key: String, value: Any): Rule = when (key) {
        "temperature" -> IsingModel((value as Number).toDouble().coerceIn(0.1, 10.0), magneticField, exchangeJ)
        "magneticField" -> IsingModel(temperature, (value as Number).toDouble().coerceIn(-2.0, 2.0), exchangeJ)
        "exchangeJ" -> IsingModel(temperature, magneticField, (value as Number).toDouble().coerceIn(-2.0, 2.0))
        else -> this
    }
}

/**
 * Rock-Paper-Scissors cyclic competition.
 */
class RockPaperScissors : IntegerRule() {
    override val numStates = 3
    override val displayName = "Rock Paper Scissors"
    override val description = "Cyclic predator-prey model"
    override val category = RuleCategory.BIOLOGICAL
    override val compatibleLatticeNames = listOf("Square (Moore)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val state = cell.currentState.toInt()
        val predator = (state + 1) % 3
        var predatorCount = 0
        for (n in neighbors) {
            if (n.currentState.toInt() == predator) predatorCount++
        }
        return if (predatorCount >= 3) IntegerCellState(predator) else IntegerCellState(state)
    }

    override fun createInitialState(): CellState = IntegerCellState(0)
}

/**
 * Brian's Brain — a 3-state cellular automaton.
 * States: 0 = off, 1 = on, 2 = dying
 */
class BriansBrain : IntegerRule() {
    override val numStates = 3
    override val displayName = "Brian's Brain"
    override val description = "3-state excitable medium"
    override val category = RuleCategory.LIFE_LIKE
    override val compatibleLatticeNames = listOf("Square (Moore)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        return when (cell.currentState.toInt()) {
            ON -> IntegerCellState(DYING)
            DYING -> IntegerCellState(OFF)
            else -> {
                var onCount = 0
                for (n in neighbors) {
                    if (n.currentState.toInt() == ON) onCount++
                }
                IntegerCellState(if (onCount == 2) ON else OFF)
            }
        }
    }

    override fun createInitialState(): CellState = IntegerCellState(0)

    companion object {
        const val OFF = 0; const val ON = 1; const val DYING = 2
    }
}

/**
 * Wireworld — models electronic circuits.
 * States: 0 = empty, 1 = electron head, 2 = electron tail, 3 = conductor
 */
class Wireworld : IntegerRule() {
    override val numStates = 4
    override val displayName = "Wireworld"
    override val description = "Electronic circuit simulation"
    override val category = RuleCategory.PHYSICS
    override val compatibleLatticeNames = listOf("Square (Moore)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        return when (cell.currentState.toInt()) {
            EMPTY -> IntegerCellState(EMPTY)
            HEAD -> IntegerCellState(TAIL)
            TAIL -> IntegerCellState(CONDUCTOR)
            CONDUCTOR -> {
                var headCount = 0
                for (n in neighbors) {
                    if (n.currentState.toInt() == HEAD) headCount++
                }
                IntegerCellState(if (headCount in 1..2) HEAD else CONDUCTOR)
            }
            else -> IntegerCellState(cell.currentState.toInt())
        }
    }

    override fun createInitialState(): CellState = IntegerCellState(0)

    companion object {
        const val EMPTY = 0; const val HEAD = 1; const val TAIL = 2; const val CONDUCTOR = 3
    }
}
