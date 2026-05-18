package org.caexplorer.domain.rule.implementations

import org.caexplorer.domain.cell.Cell
import org.caexplorer.domain.cellstate.CellState
import org.caexplorer.domain.cellstate.IntegerCellState
import org.caexplorer.domain.cellstate.RealValuedState
import org.caexplorer.domain.rule.BinaryRule
import org.caexplorer.domain.rule.IntegerRule
import org.caexplorer.domain.rule.RealRule
import org.caexplorer.domain.rule.RuleCategory
import kotlin.random.Random

/**
 * Cyclic Cellular Automaton.
 * Each cell advances to the next state if at least one neighbor is in the next state.
 */
class CyclicCA(override val numStates: Int = 14) : IntegerRule() {
    override val displayName = "Cyclic CA"
    override val description = "Cyclic cellular automaton with $numStates states"
    override val category = RuleCategory.OTHER
    override val compatibleLatticeNames = listOf("Square (Moore)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val state = cell.currentState.toInt()
        val nextState = (state + 1) % numStates
        for (n in neighbors) {
            if (n.currentState.toInt() == nextState) return IntegerCellState(nextState)
        }
        return IntegerCellState(state)
    }

    override fun createInitialState(): CellState = IntegerCellState(0)
}

/**
 * Forest Fire simulation.
 * States: 0 = empty, 1 = tree, 2 = burning
 */
class ForestFire(
    val growthProbability: Double = 0.01,
    val lightningProbability: Double = 0.0001
) : IntegerRule() {
    override val numStates = 3
    override val displayName = "Forest Fire"
    override val description = "Forest fire model (p=$growthProbability, f=$lightningProbability)"
    override val category = RuleCategory.PROBABILISTIC
    override val compatibleLatticeNames = listOf("Square (Moore)", "Square (Von Neumann)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val state = cell.currentState.toInt()
        val nextVal = when (state) {
            BURNING -> EMPTY
            TREE -> {
                val hasFireNeighbor = neighbors.any { it.currentState.toInt() == BURNING }
                if (hasFireNeighbor) BURNING
                else if (Random.nextDouble() < lightningProbability) BURNING
                else TREE
            }
            else -> {
                if (Random.nextDouble() < growthProbability) TREE else EMPTY
            }
        }
        return IntegerCellState(nextVal)
    }

    override fun createInitialState(): CellState = IntegerCellState(0)

    companion object {
        const val EMPTY = 0
        const val TREE = 1
        const val BURNING = 2
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
class IsingModel(val temperature: Double = 2.27) : BinaryRule() {
    override val displayName = "Ising Model"
    override val description = "Ising model at T=$temperature"
    override val category = RuleCategory.PHYSICS
    override val compatibleLatticeNames = listOf("Square (Moore)", "Square (Von Neumann)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val spin = if (cell.currentState.toInt() == 1) 1 else -1
        var neighborSum = 0
        for (n in neighbors) {
            neighborSum += if (n.currentState.toInt() == 1) 1 else -1
        }
        val deltaE = 2.0 * spin * neighborSum
        val flip = if (deltaE <= 0) true
        else Random.nextDouble() < kotlin.math.exp(-deltaE / temperature)
        val newSpin = if (flip) -spin else spin
        return IntegerCellState(if (newSpin == 1) 1 else 0)
    }

    override fun createInitialState(): CellState = IntegerCellState(0)
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
