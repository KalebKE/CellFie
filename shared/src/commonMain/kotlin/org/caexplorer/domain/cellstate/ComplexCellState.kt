package org.caexplorer.domain.cellstate

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * A complex number with real and imaginary parts.
 */
data class Complex(val real: Double, val imaginary: Double) {
    operator fun plus(other: Complex) = Complex(real + other.real, imaginary + other.imaginary)
    operator fun minus(other: Complex) = Complex(real - other.real, imaginary - other.imaginary)
    operator fun times(other: Complex) = Complex(
        real * other.real - imaginary * other.imaginary,
        real * other.imaginary + imaginary * other.real
    )
    operator fun times(scalar: Double) = Complex(real * scalar, imaginary * scalar)

    val modulus: Double get() = sqrt(real * real + imaginary * imaginary)
    val argument: Double get() = atan2(imaginary, real)
    val conjugate: Complex get() = Complex(real, -imaginary)

    override fun toString(): String {
        val sign = if (imaginary >= 0) "+" else ""
        return "$real${sign}${imaginary}i"
    }

    companion object {
        val ZERO = Complex(0.0, 0.0)
        val ONE = Complex(1.0, 0.0)
        val I = Complex(0.0, 1.0)

        fun fromPolar(modulus: Double, argument: Double) =
            Complex(modulus * cos(argument), modulus * sin(argument))

        fun parse(s: String): Complex {
            val trimmed = s.trim().removeSuffix("i")
            val idx = trimmed.indexOfLast { it == '+' || it == '-' }
            return if (idx <= 0) {
                Complex(trimmed.toDoubleOrNull() ?: 0.0, 0.0)
            } else {
                Complex(
                    trimmed.substring(0, idx).toDouble(),
                    trimmed.substring(idx).toDouble()
                )
            }
        }
    }
}

/**
 * Cell state for complex-valued cellular automata.
 *
 * Port of Java ComplexState.
 */
class ComplexCellState(
    var state: Complex,
    val alternateStateValue: Complex,
    val emptyStateValue: Complex,
    val fullStateValue: Complex
) : CellState {

    override var isTagged: Boolean = false
    override val taggingObjects: MutableList<Any> = mutableListOf()

    override fun copy(): ComplexCellState = ComplexCellState(
        state.copy(), alternateStateValue.copy(), emptyStateValue.copy(), fullStateValue.copy()
    ).also {
        it.isTagged = isTagged
        it.taggingObjects.addAll(taggingObjects)
    }

    override val isEmpty: Boolean get() = state == emptyStateValue
    override val isFull: Boolean get() = state == fullStateValue
    override val isAlternate: Boolean get() = state == alternateStateValue

    override fun toInt(): Int = state.hashCode()
    override val value: Any get() = state

    override fun setToEmpty() { state = emptyStateValue.copy() }
    override fun setToFull() { state = fullStateValue.copy() }
    override fun setToAlternate() { state = alternateStateValue.copy() }

    override fun setToRandom(probability: Double, random: Random) {
        val real = emptyStateValue.real + (alternateStateValue.real - emptyStateValue.real) * random.nextDouble()
        val imag = emptyStateValue.imaginary + (alternateStateValue.imaginary - emptyStateValue.imaginary) * random.nextDouble()
        state = Complex(real, imag)
    }

    override fun setFromString(value: String) {
        state = Complex.parse(value)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ComplexCellState) return false
        return state == other.state
    }

    override fun hashCode(): Int = state.hashCode()

    override fun toString(): String = state.toString()
}
