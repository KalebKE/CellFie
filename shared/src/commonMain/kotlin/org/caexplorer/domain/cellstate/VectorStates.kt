package org.caexplorer.domain.cellstate

import kotlin.random.Random

/**
 * Cell state for vector-of-integers cellular automata (e.g., lattice gas).
 *
 * Port of Java IntegerVectorState.
 */
open class IntegerVectorState(
    private var elements: IntArray,
    val minValue: Int = DEFAULT_MIN_VALUE,
    val maxValue: Int = DEFAULT_MAX_VALUE
) : CellState {

    init {
        for (e in elements) {
            require(e in minValue..maxValue) {
                "Element $e out of range [$minValue, $maxValue]"
            }
        }
    }

    override var isTagged: Boolean = false
    override val taggingObjects: MutableList<Any> = mutableListOf()

    override fun copy(): IntegerVectorState = IntegerVectorState(
        elements.copyOf(), minValue, maxValue
    ).also {
        it.isTagged = isTagged
        it.taggingObjects.addAll(taggingObjects)
    }

    val length: Int get() = elements.size
    fun getElement(index: Int): Int = elements[index]
    fun setElement(index: Int, value: Int) { elements[index] = value }
    fun getElements(): IntArray = elements

    override val isEmpty: Boolean get() = elements.all { it == minValue }
    override val isFull: Boolean get() = elements.all { it == maxValue }
    override val isAlternate: Boolean get() = !isFull && !isEmpty

    override fun toInt(): Int = elements.contentHashCode()
    override val value: Any get() = elements

    override fun setToEmpty() { elements.fill(minValue) }
    override fun setToFull() { elements.fill(maxValue) }
    override fun setToAlternate() { setToRandom(0.5) }

    override fun setToRandom(probability: Double, random: Random) {
        for (i in elements.indices) {
            elements[i] = if (random.nextDouble() < probability) {
                random.nextInt(minValue, maxValue + 1)
            } else minValue
        }
    }

    override fun setFromString(value: String) {
        val tokens = value.trim().split(Regex("[,\\s]+"))
        for (i in elements.indices.take(tokens.size)) {
            elements[i] = tokens[i].toInt()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IntegerVectorState) return false
        return elements.contentEquals(other.elements)
    }

    override fun hashCode(): Int = elements.contentHashCode()

    override fun toString(): String = elements.joinToString(" ")

    companion object {
        const val DEFAULT_MIN_VALUE = 0
        const val DEFAULT_MAX_VALUE = 1
    }
}

/**
 * Cell state for lattice gas automata (always a 7-element integer vector).
 * Elements 0-5 are particle directions, element 6 is a wall flag.
 *
 * Port of Java LatticeGasState.
 */
class LatticeGasState(
    elements: IntArray = IntArray(VECTOR_LENGTH)
) : IntegerVectorState(elements, 0, 1) {

    override fun copy(): LatticeGasState = LatticeGasState(getElements().copyOf()).also {
        it.isTagged = isTagged
        it.taggingObjects.addAll(taggingObjects)
    }

    override val isAlternate: Boolean get() = getElement(6) == 1 // Wall
    override val isFull: Boolean get() = (0 until 6).any { getElement(it) != 0 }

    override fun setToAlternate() { setElement(6, 1) } // Set wall

    override fun setToFull() {
        setToRandom(0.5)
    }

    override fun setToRandom(probability: Double, random: Random) {
        for (i in 0 until 6) {
            setElement(i, if (random.nextDouble() < probability) 1 else 0)
        }
        setElement(6, 0) // Never randomly create a wall
    }

    companion object {
        const val VECTOR_LENGTH = 7
    }
}
