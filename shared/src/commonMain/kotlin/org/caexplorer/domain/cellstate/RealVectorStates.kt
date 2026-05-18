package org.caexplorer.domain.cellstate

import kotlin.random.Random

/**
 * Cell state for real-valued vector cellular automata.
 *
 * Port of Java RealValuedVectorState.
 */
class RealValuedVectorState(
    private var elements: DoubleArray,
    val minValue: Double = DEFAULT_MIN_VALUE,
    val maxValue: Double = DEFAULT_MAX_VALUE
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

    override fun copy(): RealValuedVectorState = RealValuedVectorState(
        elements.copyOf(), minValue, maxValue
    ).also {
        it.isTagged = isTagged
        it.taggingObjects.addAll(taggingObjects)
    }

    val length: Int get() = elements.size
    fun getElement(index: Int): Double = elements[index]
    fun setElement(index: Int, value: Double) { elements[index] = value }
    fun getElements(): DoubleArray = elements

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
            elements[i] = minValue + (maxValue - minValue) * probability * random.nextDouble()
        }
    }

    override fun setFromString(value: String) {
        val tokens = value.trim().split(Regex("[,\\s]+"))
        for (i in elements.indices.take(tokens.size)) {
            elements[i] = tokens[i].toDouble()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RealValuedVectorState) return false
        return elements.contentEquals(other.elements)
    }

    override fun hashCode(): Int = elements.contentHashCode()

    override fun toString(): String = elements.joinToString(" ")

    companion object {
        const val DEFAULT_MIN_VALUE = -Double.MAX_VALUE
        const val DEFAULT_MAX_VALUE = Double.MAX_VALUE
    }
}

/**
 * Cell state for complex-valued vector cellular automata.
 *
 * Port of Java ComplexValuedVectorState.
 */
class ComplexVectorState(
    var elements: Array<Complex>,
    val alternateElements: Array<Complex>,
    val emptyElements: Array<Complex>,
    val fullElements: Array<Complex>,
    val minRandom: Double = 0.0,
    val maxRandom: Double = 1.0
) : CellState {

    init {
        require(elements.size == alternateElements.size &&
                elements.size == emptyElements.size &&
                elements.size == fullElements.size) {
            "All element arrays must have the same length"
        }
    }

    override var isTagged: Boolean = false
    override val taggingObjects: MutableList<Any> = mutableListOf()

    override fun copy(): ComplexVectorState = ComplexVectorState(
        elements.map { it.copy() }.toTypedArray(),
        alternateElements.map { it.copy() }.toTypedArray(),
        emptyElements.map { it.copy() }.toTypedArray(),
        fullElements.map { it.copy() }.toTypedArray(),
        minRandom, maxRandom
    ).also {
        it.isTagged = isTagged
        it.taggingObjects.addAll(taggingObjects)
    }

    val length: Int get() = elements.size

    override val isEmpty: Boolean get() = elements.contentEquals(emptyElements)
    override val isFull: Boolean get() = elements.contentEquals(fullElements)
    override val isAlternate: Boolean get() = elements.contentEquals(alternateElements)

    override fun toInt(): Int = elements.contentHashCode()
    override val value: Any get() = elements

    override fun setToEmpty() { emptyElements.copyInto(elements) }
    override fun setToFull() { fullElements.copyInto(elements) }
    override fun setToAlternate() { alternateElements.copyInto(elements) }

    override fun setToRandom(probability: Double, random: Random) {
        for (i in elements.indices) {
            val range = maxRandom - minRandom
            val real = minRandom + range * random.nextDouble() * probability
            val imag = minRandom + range * random.nextDouble() * probability
            elements[i] = Complex(real, imag)
        }
    }

    override fun setFromString(value: String) {
        val tokens = value.split(",").map { it.trim() }
        for (i in elements.indices.take(tokens.size)) {
            elements[i] = Complex.parse(tokens[i])
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ComplexVectorState) return false
        return elements.contentEquals(other.elements)
    }

    override fun hashCode(): Int = elements.contentHashCode()

    override fun toString(): String = elements.joinToString(", ")
}
