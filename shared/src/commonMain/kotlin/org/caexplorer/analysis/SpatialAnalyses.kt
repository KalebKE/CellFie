package org.caexplorer.analysis

import org.caexplorer.domain.lattice.Lattice
import org.caexplorer.domain.rule.Rule

private const val MAX_HISTORY = 500

/**
 * Count connected clusters of same-state cells using flood fill.
 * Reports number of clusters, average cluster size, and largest cluster.
 */
class ClusterAnalysis : Analysis {
    override val displayName = "Clusters"
    override val description = "Connected component analysis of same-state cells"
    override val category = AnalysisCategory.SPATIAL

    private val clusterCountHistory = mutableListOf<Double>()
    private val largestClusterHistory = mutableListOf<Double>()

    override fun analyze(lattice: Lattice, rule: Rule, generation: Long): List<AnalysisResult> {
        val total = lattice.cellCount
        if (total == 0) return emptyList()

        val width = lattice.width
        val height = lattice.height
        val visited = BooleanArray(total)
        val clusterSizes = mutableListOf<Int>()

        // Flood-fill to find connected components (4-connected for 2D)
        for (i in 0 until total) {
            if (visited[i]) continue
            val targetState = lattice.cells[i].currentState.toInt()
            // Skip empty-state clusters for cleaner analysis
            if (lattice.cells[i].currentState.isEmpty) {
                visited[i] = true
                continue
            }

            var size = 0
            val stack = mutableListOf(i)
            while (stack.isNotEmpty()) {
                val idx = stack.removeLast()
                if (idx < 0 || idx >= total || visited[idx]) continue
                if (lattice.cells[idx].currentState.toInt() != targetState) continue

                visited[idx] = true
                size++

                // Add 4-connected neighbors by coordinate
                val row = idx / width
                val col = idx % width
                if (row > 0) stack.add((row - 1) * width + col)
                if (row < height - 1) stack.add((row + 1) * width + col)
                if (col > 0) stack.add(row * width + (col - 1))
                if (col < width - 1) stack.add(row * width + (col + 1))
            }

            if (size > 0) clusterSizes.add(size)
        }

        val numClusters = clusterSizes.size
        val avgSize = if (numClusters > 0) clusterSizes.average() else 0.0
        val largest = clusterSizes.maxOrNull() ?: 0

        clusterCountHistory.add(numClusters.toDouble())
        if (clusterCountHistory.size > MAX_HISTORY) clusterCountHistory.removeAt(0)

        largestClusterHistory.add(largest.toDouble())
        if (largestClusterHistory.size > MAX_HISTORY) largestClusterHistory.removeAt(0)

        return listOf(
            AnalysisResult.SingleValue("Clusters", numClusters.toDouble()),
            AnalysisResult.SingleValue("Avg Size", avgSize),
            AnalysisResult.SingleValue("Largest", largest.toDouble(), "cells"),
            AnalysisResult.TimeSeries("Cluster Count", clusterCountHistory.toList()),
            AnalysisResult.TimeSeries("Largest Cluster", largestClusterHistory.toList())
        )
    }

    override fun reset() {
        clusterCountHistory.clear()
        largestClusterHistory.clear()
    }
}

/**
 * Compute center of mass of alive (non-empty) cells.
 * Track movement over time.
 */
class CenterOfMassAnalysis : Analysis {
    override val displayName = "Center of Mass"
    override val description = "Center of mass of alive cells and its movement"
    override val category = AnalysisCategory.SPATIAL

    private val xHistory = mutableListOf<Double>()
    private val yHistory = mutableListOf<Double>()

    override fun analyze(lattice: Lattice, rule: Rule, generation: Long): List<AnalysisResult> {
        val width = lattice.width
        val total = lattice.cellCount
        if (total == 0) return emptyList()

        var sumX = 0.0
        var sumY = 0.0
        var aliveCount = 0

        for (cell in lattice.cells) {
            if (!cell.currentState.isEmpty) {
                sumX += cell.coordinate.col
                sumY += cell.coordinate.row
                aliveCount++
            }
        }

        if (aliveCount == 0) return listOf(
            AnalysisResult.SingleValue("CoM X", 0.0),
            AnalysisResult.SingleValue("CoM Y", 0.0)
        )

        val comX = sumX / aliveCount
        val comY = sumY / aliveCount

        xHistory.add(comX)
        if (xHistory.size > MAX_HISTORY) xHistory.removeAt(0)

        yHistory.add(comY)
        if (yHistory.size > MAX_HISTORY) yHistory.removeAt(0)

        return listOf(
            AnalysisResult.SingleValue("CoM X", comX),
            AnalysisResult.SingleValue("CoM Y", comY),
            AnalysisResult.TimeSeries("CoM X", xHistory.toList()),
            AnalysisResult.TimeSeries("CoM Y", yHistory.toList())
        )
    }

    override fun reset() {
        xHistory.clear()
        yHistory.clear()
    }
}
