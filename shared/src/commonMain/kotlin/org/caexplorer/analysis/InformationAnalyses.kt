package org.caexplorer.analysis

import org.caexplorer.domain.lattice.Lattice
import org.caexplorer.domain.rule.IntegerRule
import org.caexplorer.domain.rule.Rule
import kotlin.math.ln

private const val MAX_HISTORY = 500
private const val LN2 = 0.6931471805599453

/**
 * Mutual information between a cell and its neighbors.
 * Measures spatial correlations in the lattice.
 * MI(X;Y) = H(X) + H(Y) - H(X,Y)
 */
class MutualInformationAnalysis : Analysis {
    override val displayName = "Mutual Information"
    override val description = "Spatial correlation between cells and neighbors"
    override val category = AnalysisCategory.INFORMATION

    private val history = mutableListOf<Double>()

    override fun analyze(lattice: Lattice, rule: Rule, generation: Long): List<AnalysisResult> {
        val total = lattice.cellCount
        if (total == 0) return emptyList()

        val numStates = (rule as? IntegerRule)?.numStates ?: 2

        // Count single-cell state frequencies
        val cellCounts = IntArray(numStates)
        // Count joint (cell, neighbor) state frequencies
        val jointCounts = Array(numStates) { IntArray(numStates) }
        // Count neighbor state frequencies
        val neighborCounts = IntArray(numStates)
        var totalPairs = 0

        for (cell in lattice.cells) {
            val cellState = cell.currentState.toInt().coerceIn(0, numStates - 1)
            cellCounts[cellState]++

            val neighbors = lattice.getNeighbors(cell)
            for (neighbor in neighbors) {
                val neighborState = neighbor.currentState.toInt().coerceIn(0, numStates - 1)
                jointCounts[cellState][neighborState]++
                neighborCounts[neighborState]++
                totalPairs++
            }
        }

        if (totalPairs == 0) return emptyList()

        // H(X) - entropy of cell states
        var hX = 0.0
        for (count in cellCounts) {
            if (count > 0) {
                val p = count.toDouble() / total
                hX -= p * (ln(p) / LN2)
            }
        }

        // H(Y) - entropy of neighbor states
        var hY = 0.0
        for (count in neighborCounts) {
            if (count > 0) {
                val p = count.toDouble() / totalPairs
                hY -= p * (ln(p) / LN2)
            }
        }

        // H(X,Y) - joint entropy
        var hXY = 0.0
        for (i in 0 until numStates) {
            for (j in 0 until numStates) {
                val count = jointCounts[i][j]
                if (count > 0) {
                    val p = count.toDouble() / totalPairs
                    hXY -= p * (ln(p) / LN2)
                }
            }
        }

        val mi = (hX + hY - hXY).coerceAtLeast(0.0)

        history.add(mi)
        if (history.size > MAX_HISTORY) history.removeAt(0)

        return listOf(
            AnalysisResult.SingleValue("Mutual Info", mi, "bits"),
            AnalysisResult.TimeSeries("Mutual Information", history.toList())
        )
    }

    override fun reset() {
        history.clear()
    }
}

/**
 * Hamming distance between current and previous generation.
 * Counts the number of cells that changed state.
 */
class HammingDistanceAnalysis : Analysis {
    override val displayName = "Hamming Distance"
    override val description = "Number of cells that changed between generations"
    override val category = AnalysisCategory.INFORMATION

    private val history = mutableListOf<Double>()

    override fun analyze(lattice: Lattice, rule: Rule, generation: Long): List<AnalysisResult> {
        val total = lattice.cellCount
        if (total == 0) return emptyList()

        var distance = 0
        for (cell in lattice.cells) {
            val prev = cell.previousState
            if (prev != null && cell.currentState.toInt() != prev.toInt()) {
                distance++
            }
        }

        history.add(distance.toDouble())
        if (history.size > MAX_HISTORY) history.removeAt(0)

        return listOf(
            AnalysisResult.SingleValue("Hamming Distance", distance.toDouble()),
            AnalysisResult.SingleValue("Change Rate", if (total > 0) distance.toDouble() / total else 0.0),
            AnalysisResult.TimeSeries("Hamming Distance", history.toList())
        )
    }

    override fun reset() {
        history.clear()
    }
}
