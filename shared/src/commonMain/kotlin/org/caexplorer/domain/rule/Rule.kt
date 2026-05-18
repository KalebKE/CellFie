package org.caexplorer.domain.rule

import org.caexplorer.domain.cell.Cell
import org.caexplorer.domain.cellstate.CellState

/**
 * Describes a configurable property of a rule.
 */
sealed class RuleProperty(val key: String, val label: String, val description: String = "") {
    class IntProperty(key: String, label: String, val value: Int, val min: Int, val max: Int, description: String = "") : RuleProperty(key, label, description)
    class FloatProperty(key: String, label: String, val value: Float, val min: Float, val max: Float, description: String = "") : RuleProperty(key, label, description)
    class BooleanProperty(key: String, label: String, val value: Boolean, description: String = "") : RuleProperty(key, label, description)
    class ChoiceProperty(key: String, label: String, val value: String, val choices: List<String>, description: String = "") : RuleProperty(key, label, description)
}

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

    /** Configurable properties for this rule. Empty list means no configuration. */
    val properties: List<RuleProperty> get() = emptyList()

    /** Update a property value. Returns a new Rule instance with the updated value, or this if unchanged. */
    fun withProperty(key: String, value: Any): Rule = this
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
