package org.caexplorer.analysis

import org.caexplorer.domain.lattice.Lattice
import org.caexplorer.domain.rule.Rule

/**
 * Result data from an analysis computation.
 */
sealed class AnalysisResult {
    data class SingleValue(val label: String, val value: Double, val unit: String = "") : AnalysisResult()
    data class TimeSeries(val label: String, val values: List<Double>, val maxPoints: Int = 200) : AnalysisResult()
    data class Histogram(val label: String, val bins: Map<String, Double>) : AnalysisResult()
    data class Grid(val label: String, val values: List<List<Double>>, val width: Int, val height: Int) : AnalysisResult()
}

/**
 * Base interface for all CA analysis tools.
 */
interface Analysis {
    val displayName: String
    val description: String
    val category: AnalysisCategory

    /**
     * Compute the analysis for the current lattice state.
     * Called after each generation (or on-demand).
     */
    fun analyze(lattice: Lattice, rule: Rule, generation: Long): List<AnalysisResult>

    /**
     * Reset accumulated data (e.g., time series history).
     */
    fun reset()
}

enum class AnalysisCategory(val displayName: String) {
    STATISTICAL("Statistical"),
    SPATIAL("Spatial"),
    INFORMATION("Information Theory"),
    DYNAMICS("Dynamics"),
}
