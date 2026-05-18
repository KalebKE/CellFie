package org.caexplorer.domain.rule

import org.caexplorer.domain.cell.Cell
import org.caexplorer.domain.cellstate.CellState

/**
 * Defines a cellular automaton rule — the function that computes
 * the next state of a cell based on its current state and neighbors.
 *
 * Rules are the heart of CA simulation. Each rule defines:
 * - How to compute the next generation
 * - What cell states and lattice types are compatible
 * - Display metadata (name, description, category)
 *
 * Port of Java Rule abstract class.
 */
interface Rule {
    /** Display name shown in the UI. */
    val displayName: String

    /** Short description of the rule's behavior. */
    val description: String

    /** Category for grouping in the rule browser. */
    val category: RuleCategory

    /**
     * Calculate the next state for a cell given its neighbors.
     * This is the HOT PATH — called for every cell every generation.
     */
    fun nextState(cell: Cell, neighbors: Array<Cell>): CellState

    /**
     * Number of initial generations required before the rule can compute.
     * Most rules need 1 (just the current state), but some need 2+.
     */
    val requiredGenerations: Int get() = 1

    /**
     * Create a fresh initial cell state for this rule.
     */
    fun createInitialState(): CellState

    /**
     * The names of compatible lattice types for this rule.
     */
    val compatibleLatticeNames: List<String>

    /**
     * Tooltip text for the UI.
     */
    val tooltip: String get() = description
}

/**
 * Categories for organizing rules in the UI.
 */
enum class RuleCategory(val displayName: String) {
    ELEMENTARY("Elementary (1D)"),
    LIFE_LIKE("Life-like"),
    TOTALISTIC("Totalistic"),
    CONTINUOUS("Continuous"),
    COMPLEX("Complex-valued"),
    LATTICE_GAS("Lattice Gas"),
    PROBABILISTIC("Probabilistic"),
    PHYSICS("Physics"),
    BIOLOGICAL("Biological"),
    SOCIAL("Social"),
    FRACTAL("Fractal"),
    NEURAL("Neural"),
    CUSTOM("Custom"),
    OTHER("Other")
}

/**
 * Base class for rules operating on integer cell states.
 * Most CA rules extend this.
 */
abstract class IntegerRule : Rule {
    /** Number of possible integer states (e.g., 2 for binary, 256 for 8-bit). */
    abstract val numStates: Int
}

/**
 * Base class for rules operating on real-valued cell states.
 */
abstract class RealRule : Rule

/**
 * Base class for rules operating on complex-valued cell states.
 */
abstract class ComplexRule : Rule

/**
 * Base class for rules operating on binary (0/1) cell states.
 */
abstract class BinaryRule : IntegerRule() {
    override val numStates: Int = 2
}
