package org.caexplorer.analysis

import org.caexplorer.domain.lattice.Lattice
import org.caexplorer.domain.rule.Rule
import kotlin.math.ln
import kotlin.math.sqrt

private const val MAX_HISTORY = 500
private const val LN2 = 0.6931471805599453

/**
 * Estimates fractal dimension using the box-counting method.
 * Covers the lattice with boxes of decreasing size and counts
 * how many boxes contain at least one alive cell.
 * The slope of log(N(s)) vs log(1/s) estimates the fractal dimension.
 */
class BoxCountingDimensionAnalysis : Analysis {
    override val displayName = "Box-Counting Dimension"
    override val description = "Fractal dimension estimate via box-counting method"
    override val category = AnalysisCategory.DIMENSIONAL

    private val history = mutableListOf<Double>()

    override fun analyze(lattice: Lattice, rule: Rule, generation: Long): List<AnalysisResult> {
        val width = lattice.width
        val height = lattice.height
        if (width == 0 || height == 0) return emptyList()

        val boxSizes = listOf(2, 4, 8, 16, 32, 64).filter { it <= maxOf(width, height) }
        if (boxSizes.size < 2) {
            history.add(0.0)
            if (history.size > MAX_HISTORY) history.removeAt(0)
            return listOf(
                AnalysisResult.SingleValue("Fractal Dimension", 0.0),
                AnalysisResult.TimeSeries("Fractal Dimension", history.toList())
            )
        }

        val logInvS = mutableListOf<Double>()
        val logN = mutableListOf<Double>()

        for (boxSize in boxSizes) {
            var count = 0
            val boxCols = (width + boxSize - 1) / boxSize
            val boxRows = (height + boxSize - 1) / boxSize

            for (br in 0 until boxRows) {
                for (bc in 0 until boxCols) {
                    var found = false
                    val rStart = br * boxSize
                    val rEnd = minOf(rStart + boxSize, height)
                    val cStart = bc * boxSize
                    val cEnd = minOf(cStart + boxSize, width)
                    for (r in rStart until rEnd) {
                        if (found) break
                        for (c in cStart until cEnd) {
                            val idx = r * width + c
                            if (idx < lattice.cellCount && !lattice.cells[idx].currentState.isEmpty) {
                                found = true
                                break
                            }
                        }
                    }
                    if (found) count++
                }
            }

            if (count > 0) {
                logInvS.add(ln(1.0 / boxSize) / LN2)
                logN.add(ln(count.toDouble()) / LN2)
            }
        }

        val dimension = if (logInvS.size >= 2) linearRegressionSlope(logInvS, logN) else 0.0

        history.add(dimension)
        if (history.size > MAX_HISTORY) history.removeAt(0)

        return listOf(
            AnalysisResult.SingleValue("Fractal Dimension", dimension),
            AnalysisResult.TimeSeries("Fractal Dimension", history.toList())
        )
    }

    override fun reset() {
        history.clear()
    }
}

/**
 * Estimates correlation dimension from spatial pair correlations.
 * Samples alive cells and counts pairs within increasing radii.
 * The slope of log(C(r)) vs log(r) gives the correlation dimension.
 */
class CorrelationDimensionAnalysis : Analysis {
    override val displayName = "Correlation Dimension"
    override val description = "Correlation dimension from spatial pair distances"
    override val category = AnalysisCategory.DIMENSIONAL

    private val history = mutableListOf<Double>()

    override fun analyze(lattice: Lattice, rule: Rule, generation: Long): List<AnalysisResult> {
        val width = lattice.width
        if (width == 0) return emptyList()

        // Collect alive cell positions
        val aliveCells = mutableListOf<Pair<Int, Int>>()
        for (cell in lattice.cells) {
            if (!cell.currentState.isEmpty) {
                aliveCells.add(cell.coordinate.row to cell.coordinate.col)
            }
        }

        val n = aliveCells.size
        if (n < 2) {
            history.add(0.0)
            if (history.size > MAX_HISTORY) history.removeAt(0)
            return listOf(
                AnalysisResult.SingleValue("Correlation Dim", 0.0),
                AnalysisResult.TimeSeries("Correlation Dimension", history.toList())
            )
        }

        // Sample up to 200 cells for performance
        val sampled = if (n <= 200) aliveCells else {
            val step = n / 200
            (0 until 200).map { aliveCells[(it * step).coerceAtMost(n - 1)] }
        }
        val sampleN = sampled.size
        val totalPairs = sampleN.toLong() * (sampleN - 1) / 2

        val radii = listOf(1.0, 2.0, 4.0, 8.0, 16.0, 32.0)
        val logR = mutableListOf<Double>()
        val logC = mutableListOf<Double>()

        for (r in radii) {
            var pairsWithin = 0L
            val rSq = r * r
            for (i in 0 until sampleN) {
                for (j in i + 1 until sampleN) {
                    val dr = (sampled[i].first - sampled[j].first).toDouble()
                    val dc = (sampled[i].second - sampled[j].second).toDouble()
                    if (dr * dr + dc * dc <= rSq) {
                        pairsWithin++
                    }
                }
            }

            if (pairsWithin > 0 && totalPairs > 0) {
                val cr = pairsWithin.toDouble() / totalPairs
                logR.add(ln(r) / LN2)
                logC.add(ln(cr) / LN2)
            }
        }

        val dimension = if (logR.size >= 2) linearRegressionSlope(logR, logC) else 0.0

        history.add(dimension)
        if (history.size > MAX_HISTORY) history.removeAt(0)

        return listOf(
            AnalysisResult.SingleValue("Correlation Dim", dimension),
            AnalysisResult.TimeSeries("Correlation Dimension", history.toList())
        )
    }

    override fun reset() {
        history.clear()
    }
}

/**
 * Finds the largest contiguous neighborhood of same-state cells
 * using flood fill, tracking all states including empty.
 */
class LargestNeighborhoodAnalysis : Analysis {
    override val displayName = "Largest Neighborhood"
    override val description = "Largest connected region per cell state"
    override val category = AnalysisCategory.DIMENSIONAL

    private val largestOverallHistory = mutableListOf<Double>()

    override fun analyze(lattice: Lattice, rule: Rule, generation: Long): List<AnalysisResult> {
        val total = lattice.cellCount
        if (total == 0) return emptyList()

        val width = lattice.width
        val height = lattice.height
        val visited = BooleanArray(total)
        val largestByState = mutableMapOf<Int, Int>()

        for (i in 0 until total) {
            if (visited[i]) continue
            val targetState = lattice.cells[i].currentState.toInt()

            var size = 0
            val stack = mutableListOf(i)
            while (stack.isNotEmpty()) {
                val idx = stack.removeLast()
                if (idx < 0 || idx >= total || visited[idx]) continue
                if (lattice.cells[idx].currentState.toInt() != targetState) continue

                visited[idx] = true
                size++

                val row = idx / width
                val col = idx % width
                if (row > 0) stack.add((row - 1) * width + col)
                if (row < height - 1) stack.add((row + 1) * width + col)
                if (col > 0) stack.add(row * width + (col - 1))
                if (col < width - 1) stack.add(row * width + (col + 1))
            }

            if (size > 0) {
                largestByState[targetState] = maxOf(largestByState[targetState] ?: 0, size)
            }
        }

        val results = mutableListOf<AnalysisResult>()
        for ((state, size) in largestByState.entries.sortedByDescending { it.value }) {
            results.add(AnalysisResult.SingleValue("State $state Largest", size.toDouble(), "cells"))
        }

        val overallLargest = (largestByState.values.maxOrNull() ?: 0).toDouble()
        largestOverallHistory.add(overallLargest)
        if (largestOverallHistory.size > MAX_HISTORY) largestOverallHistory.removeAt(0)

        results.add(AnalysisResult.TimeSeries("Largest Region", largestOverallHistory.toList()))

        return results
    }

    override fun reset() {
        largestOverallHistory.clear()
    }
}

/**
 * Simple linear regression: returns slope of y = mx + b fit.
 */
internal fun linearRegressionSlope(x: List<Double>, y: List<Double>): Double {
    val n = x.size
    if (n < 2) return 0.0
    val sumX = x.sum()
    val sumY = y.sum()
    val sumXY = x.zip(y).sumOf { it.first * it.second }
    val sumX2 = x.sumOf { it * it }
    val denom = n * sumX2 - sumX * sumX
    if (denom == 0.0) return 0.0
    return (n * sumXY - sumX * sumY) / denom
}
