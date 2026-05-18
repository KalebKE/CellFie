package org.caexplorer.engine

import org.caexplorer.domain.cell.Cell
import org.caexplorer.domain.cellstate.IntegerCellState
import org.caexplorer.domain.cellstate.RealValuedState
import org.caexplorer.domain.colorscheme.ColorScheme
import org.caexplorer.domain.colorscheme.RainbowColorScheme
import org.caexplorer.domain.lattice.LatticeType
import org.caexplorer.domain.lattice.createLattice
import org.caexplorer.domain.rule.*
import org.caexplorer.domain.util.Coordinate
import kotlin.random.Random

/**
 * Factory for creating simulation configurations with sensible defaults.
 * Encapsulates the complexity of wiring together rules, lattices, and color schemes.
 */
object SimulationFactory {

    /**
     * Create a SimulationConfig with the given parameters and random initialization.
     */
    fun createConfig(
        rule: Rule,
        width: Int = 200,
        height: Int = 200,
        latticeType: LatticeType = LatticeType.SQUARE_MOORE,
        colorScheme: ColorScheme = RainbowColorScheme(),
        density: Double = 0.25,
        numWorkers: Int = DEFAULT_WORKERS
    ): SimulationConfig {
        val numStates = (rule as? IntegerRule)?.numStates ?: 2
        IntegerCellState.numStates = numStates

        val isRealRule = rule is RealRule

        val lattice = createLattice(latticeType, width, height) { coord ->
            if (isRealRule) {
                Cell(RealValuedState(Random.nextDouble()), coord)
            } else {
                val state = if (Random.nextDouble() < density) {
                    if (numStates > 2) Random.nextInt(1, numStates) else 1
                } else 0
                Cell(IntegerCellState(state), coord)
            }
        }

        return SimulationConfig(
            rule = rule,
            lattice = lattice,
            colorScheme = colorScheme,
            numWorkers = numWorkers
        )
    }

    /**
     * Create a config with a center seed pattern (single cell in center).
     */
    fun createCenterSeedConfig(
        rule: Rule,
        width: Int = 200,
        height: Int = 200,
        latticeType: LatticeType = LatticeType.SQUARE_MOORE,
        colorScheme: ColorScheme = RainbowColorScheme(),
        numWorkers: Int = DEFAULT_WORKERS
    ): SimulationConfig {
        val numStates = (rule as? IntegerRule)?.numStates ?: 2
        IntegerCellState.numStates = numStates

        val lattice = createLattice(latticeType, width, height) { coord ->
            val state = if (coord.row == height / 2 && coord.col == width / 2) numStates - 1 else 0
            Cell(IntegerCellState(state), coord)
        }

        return SimulationConfig(
            rule = rule,
            lattice = lattice,
            colorScheme = colorScheme,
            numWorkers = numWorkers
        )
    }

    /**
     * Create a config with a specific state array (for loading saved simulations).
     */
    fun createFromStates(
        rule: Rule,
        width: Int,
        height: Int,
        states: IntArray,
        latticeType: LatticeType = LatticeType.SQUARE_MOORE,
        colorScheme: ColorScheme = RainbowColorScheme(),
        numWorkers: Int = DEFAULT_WORKERS
    ): SimulationConfig {
        val numStates = (rule as? IntegerRule)?.numStates ?: 2
        IntegerCellState.numStates = numStates

        val lattice = createLattice(latticeType, width, height) { coord ->
            val idx = coord.row * width + coord.col
            val state = states.getOrElse(idx) { 0 }.coerceIn(0, numStates - 1)
            Cell(IntegerCellState(state), coord)
        }

        return SimulationConfig(
            rule = rule,
            lattice = lattice,
            colorScheme = colorScheme,
            numWorkers = numWorkers
        )
    }

    /**
     * Determine a good default density for a given rule.
     */
    fun defaultDensityForRule(rule: Rule): Double = when {
        rule is BinaryRule && rule.category == RuleCategory.LIFE_LIKE -> 0.25
        rule is BinaryRule -> 0.30
        rule is IntegerRule && (rule.numStates) > 2 -> 0.50
        rule is RealRule -> 1.0
        else -> 0.25
    }
}
