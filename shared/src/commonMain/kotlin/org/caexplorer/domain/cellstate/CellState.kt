package org.caexplorer.domain.cellstate

import kotlin.random.Random

/**
 * Represents the state of a single cell in a cellular automaton.
 *
 * This is the core abstraction: each cell holds a CellState that encodes its
 * current value. States can be integers, reals, complex numbers, vectors, etc.
 *
 * Port of Java CellState abstract class.
 */
sealed interface CellState {
    /** Clone this state into an independent copy. */
    fun copy(): CellState

    /** Whether this state represents the "empty" condition. */
    val isEmpty: Boolean

    /** Whether this state represents the "full" condition. */
    val isFull: Boolean

    /** Whether this state is an alternate/intermediate value. */
    val isAlternate: Boolean

    /** Whether this state is the user-designated "draw" state. */
    val isDrawState: Boolean get() = isFull

    /** Convert to integer representation (for hashing/display). */
    fun toInt(): Int

    /** Set to the empty state. */
    fun setToEmpty()

    /** Set to the full state. */
    fun setToFull()

    /** Set to an alternate intermediate state. */
    fun setToAlternate()

    /** Set to the user-designated drawing state. */
    fun setToDrawingState() { setToFull() }

    /** Randomize this state with the given probability [0.0, 1.0]. */
    fun setToRandom(probability: Double, random: Random = Random.Default)

    /** Set state from a string representation. */
    fun setFromString(value: String)

    /** The underlying state value (for display/serialization). */
    val value: Any

    // --- Tagging support ---
    var isTagged: Boolean
    val taggingObjects: MutableList<Any>

    fun tag(taggingObject: Any) {
        isTagged = true
        taggingObjects.add(0, taggingObject)
    }

    fun untag(taggingObject: Any) {
        taggingObjects.remove(taggingObject)
        if (taggingObjects.isEmpty()) isTagged = false
    }
}
