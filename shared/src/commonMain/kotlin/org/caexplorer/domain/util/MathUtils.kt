package org.caexplorer.domain.util

import kotlin.math.sqrt
import kotlin.random.Random

/** The golden ratio constant. */
const val GOLDEN_RATIO: Double = 1.6180339887498949

/**
 * Generates Gaussian-distributed random numbers with specified mean and standard deviation.
 */
class GaussianRandom(private val random: Random = Random.Default) {
    /**
     * Returns a random double from a Gaussian distribution N(mean, σ²).
     */
    fun nextGaussian(mean: Double, standardDeviation: Double): Double {
        // Box-Muller transform for Gaussian random
        val standardRandom = nextStandardGaussian()
        return standardDeviation * standardRandom + mean
    }

    private var hasSpare = false
    private var spare = 0.0

    /**
     * Box-Muller transform producing N(0,1) random values.
     */
    private fun nextStandardGaussian(): Double {
        if (hasSpare) {
            hasSpare = false
            return spare
        }
        var u: Double
        var v: Double
        var s: Double
        do {
            u = random.nextDouble() * 2.0 - 1.0
            v = random.nextDouble() * 2.0 - 1.0
            s = u * u + v * v
        } while (s >= 1.0 || s == 0.0)
        val mul = sqrt(-2.0 * kotlin.math.ln(s) / s)
        spare = v * mul
        hasSpare = true
        return u * mul
    }
}

/**
 * Number base conversion utilities.
 */
object BaseConverter {

    fun convertFromBaseTen(num: Long, radix: Int): IntArray {
        if (num == 0L) return intArrayOf(0)
        val digits = mutableListOf<Int>()
        var remaining = num
        while (remaining > 0) {
            digits.add((remaining % radix).toInt())
            remaining /= radix
        }
        return digits.toIntArray() // index 0 = least significant
    }

    fun convertToBaseTen(number: String, radix: Int): Long {
        return number.toLong(radix)
    }

    fun convertBase(num: Long, radix: Int): String {
        if (num < radix) return num.toString()
        return convertBase(num / radix, radix) + " " + (num % radix)
    }
}

/**
 * Linear least-squares regression: y = mx + b.
 */
class LeastSquaresFit {
    var slope: Double = 0.0; private set
    var yIntercept: Double = 0.0; private set
    var rSquared: Double = 0.0; private set
    var chiSquared: Double = 0.0; private set
    var standardDeviationSlope: Double = 0.0; private set
    var standardDeviationYIntercept: Double = 0.0; private set

    /**
     * Fit a line to the given (x, y) points.
     */
    fun fit(points: List<Pair<Double, Double>>) {
        if (points.size <= 1) return

        val n = points.size.toDouble()
        var sumX = 0.0
        var sumY = 0.0
        for ((x, y) in points) {
            sumX += x
            sumY += y
        }

        var sumOfTSquared = 0.0
        slope = 0.0
        for ((x, y) in points) {
            val t = x - sumX / n
            sumOfTSquared += t * t
            slope += t * y
        }
        slope /= sumOfTSquared

        yIntercept = (sumY - sumX * slope) / n

        standardDeviationSlope = sqrt(1.0 / sumOfTSquared)
        standardDeviationYIntercept = sqrt((1.0 + sumX * sumX / (n * sumOfTSquared)) / n)

        val covariance = -sumX / (n * sumOfTSquared)
        rSquared = if (standardDeviationSlope * standardDeviationYIntercept != 0.0) {
            val r = covariance / (standardDeviationSlope * standardDeviationYIntercept)
            r * r
        } else 0.0

        chiSquared = 0.0
        for ((x, y) in points) {
            val diff = y - yIntercept - slope * x
            chiSquared += diff * diff
        }

        if (n > 2) {
            val adjustment = sqrt(chiSquared / (n - 2))
            standardDeviationSlope *= adjustment
            standardDeviationYIntercept *= adjustment
        }
    }
}
