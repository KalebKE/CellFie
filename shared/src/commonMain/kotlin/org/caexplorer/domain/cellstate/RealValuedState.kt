package org.caexplorer.domain.cellstate

import kotlin.random.Random

/**
 * Cell state for continuous real-valued cellular automata.
 * State is a double in the range [emptyState, fullState].
 *
 * Port of Java RealValuedState.
 */
class RealValuedState(
    var state: Double = DEFAULT_EMPTY_STATE,
    val emptyState: Double = DEFAULT_EMPTY_STATE,
    val fullState: Double = DEFAULT_FULL_STATE
) : CellState {

    override var isTagged: Boolean = false
    override val taggingObjects: MutableList<Any> = mutableListOf()

    override fun copy(): RealValuedState = RealValuedState(state, emptyState, fullState).also {
        it.isTagged = isTagged
        it.taggingObjects.addAll(taggingObjects)
    }

    override val isEmpty: Boolean get() = state == emptyState
    override val isFull: Boolean get() = state == fullState
    override val isAlternate: Boolean get() = !isEmpty && !isFull

    override fun toInt(): Int = state.toInt()
    override val value: Any get() = state

    override fun setToEmpty() { state = emptyState }
    override fun setToFull() { state = fullState }
    override fun setToAlternate() { setToRandom(0.5) }

    override fun setToRandom(probability: Double, random: Random) {
        state = emptyState + (fullState - emptyState) * probability * random.nextDouble()
    }

    override fun setFromString(value: String) {
        state = value.trim().toDouble()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RealValuedState) return false
        return state == other.state
    }

    override fun hashCode(): Int = state.hashCode()

    override fun toString(): String = state.toString()

    companion object {
        const val DEFAULT_EMPTY_STATE = 0.0
        const val DEFAULT_FULL_STATE = 1.0
    }
}
