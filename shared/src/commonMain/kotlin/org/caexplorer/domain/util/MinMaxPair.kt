package org.caexplorer.domain.util

/**
 * A pair of min/max double values with utility for finding extremes in collections.
 */
data class MinMaxPair(val min: Double, val max: Double) {
    companion object {
        fun findMinMax(numbers: DoubleArray): MinMaxPair? {
            if (numbers.isEmpty()) return null
            var min = Double.MAX_VALUE
            var max = -Double.MAX_VALUE
            for (n in numbers) {
                if (n < min) min = n
                if (n > max) max = n
            }
            return MinMaxPair(min, max)
        }

        fun findMinMax(numbers: Collection<Double>): MinMaxPair? {
            if (numbers.isEmpty()) return null
            return findMinMax(numbers.toDoubleArray())
        }
    }
}
