package org.caexplorer.domain.cellstate

import kotlin.random.Random

/**
 * Generic cell state that wraps any arbitrary object.
 * Used for rules with custom state types.
 *
 * Port of Java ObjectState.
 */
class ObjectCellState(
    var state: Any,
    val alternateStateValue: Any,
    val emptyStateValue: Any,
    val fullStateValue: Any
) : CellState {

    override var isTagged: Boolean = false
    override val taggingObjects: MutableList<Any> = mutableListOf()

    // Shallow copy — same object references for alternate/empty/full
    override fun copy(): ObjectCellState = ObjectCellState(
        state, alternateStateValue, emptyStateValue, fullStateValue
    ).also {
        it.isTagged = isTagged
        it.taggingObjects.addAll(taggingObjects)
    }

    override val isEmpty: Boolean get() = state == emptyStateValue
    override val isFull: Boolean get() = state == fullStateValue
    override val isAlternate: Boolean get() = state == alternateStateValue

    override fun toInt(): Int = state.hashCode()
    override val value: Any get() = state

    override fun setToEmpty() { state = emptyStateValue }
    override fun setToFull() { state = fullStateValue }
    override fun setToAlternate() { state = alternateStateValue }

    override fun setToRandom(probability: Double, random: Random) {
        state = if (random.nextDouble() < probability) fullStateValue else emptyStateValue
    }

    override fun setFromString(value: String) {
        state = when (value.trim()) {
            fullStateValue.toString() -> fullStateValue
            alternateStateValue.toString() -> alternateStateValue
            emptyStateValue.toString() -> emptyStateValue
            else -> value
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ObjectCellState) return false
        return state.toString() == other.state.toString()
    }

    override fun hashCode(): Int = state.hashCode()

    override fun toString(): String = state.toString()
}
