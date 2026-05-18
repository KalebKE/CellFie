package org.caexplorer.domain.cellstate

import kotlin.random.Random

/**
 * Cell state for integer-valued cellular automata (the most common type).
 * State values range from 0 (empty) to numStates-1 (full).
 *
 * Port of Java IntegerCellState.
 */
class IntegerCellState(
    var state: Int = 0,
) : CellState {

    override var isTagged: Boolean = false
    override val taggingObjects: MutableList<Any> = mutableListOf()

    override fun copy(): IntegerCellState = IntegerCellState(state).also {
        it.isTagged = isTagged
        it.taggingObjects.addAll(taggingObjects)
    }

    override val isEmpty: Boolean get() = state == 0
    override val isFull: Boolean get() = state == numStates - 1
    override val isAlternate: Boolean get() = state == alternateState
    override val isDrawState: Boolean get() = state == drawState

    override fun toInt(): Int = state
    override val value: Any get() = state

    override fun setToEmpty() { state = 0 }
    override fun setToFull() { state = numStates - 1 }
    override fun setToAlternate() {
        state = if (SECOND_DRAW_STATE >= 0) SECOND_DRAW_STATE else alternateState
    }

    override fun setToDrawingState() {
        state = if (DRAW_STATE >= 0) DRAW_STATE else numStates - 1
    }

    override fun setToRandom(probability: Double, random: Random) {
        state = if (random.nextDouble() < probability) {
            random.nextInt(1, numStates.coerceAtLeast(2))
        } else 0
    }

    override fun setFromString(value: String) {
        state = value.trim().toInt()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IntegerCellState) return false
        return state == other.state
    }

    override fun hashCode(): Int = state

    override fun toString(): String = state.toString()

    companion object {
        /** Total number of states for this CA configuration. */
        var numStates: Int = 2

        /** The user-selected draw state (or -1 for default). */
        var DRAW_STATE: Int = -1

        /** The user-selected secondary draw state (or -1 for default). */
        var SECOND_DRAW_STATE: Int = -1

        /** Computed alternate state (midpoint). */
        val alternateState: Int get() = (numStates / 2) - 1

        /** The effective draw state. */
        val drawState: Int get() = if (DRAW_STATE >= 0) DRAW_STATE else numStates - 1
    }
}
