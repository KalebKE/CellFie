package org.caexplorer.analysis

import org.caexplorer.domain.lattice.Lattice
import org.caexplorer.domain.rule.IntegerRule
import org.caexplorer.domain.rule.Rule
import kotlin.math.ln

private const val MAX_HISTORY = 500
private const val LN2 = 0.6931471805599453

/**
 * Tracks population of each state over time.
 * Produces a histogram of current state distribution
 * and a time-series of each state's count.
 */
class PopulationAnalysis : Analysis {
    override val displayName = "Population"
    override val description = "Count of cells in each state over time"
    override val category = AnalysisCategory.STATISTICAL

    private val stateHistories = mutableMapOf<Int, MutableList<Double>>()

    override fun analyze(lattice: Lattice, rule: Rule, generation: Long): List<AnalysisResult> {
        val counts = mutableMapOf<Int, Int>()
        for (cell in lattice.cells) {
            val s = cell.currentState.toInt()
            counts[s] = (counts[s] ?: 0) + 1
        }

        // Update time series per state
        for ((state, count) in counts) {
            val history = stateHistories.getOrPut(state) { mutableListOf() }
            history.add(count.toDouble())
            if (history.size > MAX_HISTORY) history.removeAt(0)
        }

        val results = mutableListOf<AnalysisResult>()

        // Current distribution as histogram
        val numStates = (rule as? IntegerRule)?.numStates ?: (counts.keys.maxOrNull()?.plus(1) ?: 2)
        val bins = linkedMapOf<String, Double>()
        for (s in 0 until numStates) {
            bins["State $s"] = (counts[s] ?: 0).toDouble()
        }
        results.add(AnalysisResult.Histogram("State Distribution", bins))

        // Time series for each state
        for ((state, history) in stateHistories) {
            results.add(AnalysisResult.TimeSeries("State $state", history.toList()))
        }

        return results
    }

    override fun reset() {
        stateHistories.clear()
    }
}

/**
 * Population density (fraction of non-empty cells) over time.
 */
class DensityAnalysis : Analysis {
    override val displayName = "Density"
    override val description = "Fraction of alive (non-empty) cells over time"
    override val category = AnalysisCategory.STATISTICAL

    private val history = mutableListOf<Double>()

    override fun analyze(lattice: Lattice, rule: Rule, generation: Long): List<AnalysisResult> {
        val total = lattice.cellCount
        if (total == 0) return emptyList()

        val alive = lattice.cells.count { !it.currentState.isEmpty }
        val density = alive.toDouble() / total

        history.add(density)
        if (history.size > MAX_HISTORY) history.removeAt(0)

        return listOf(
            AnalysisResult.SingleValue("Current Density", density),
            AnalysisResult.TimeSeries("Density", history.toList())
        )
    }

    override fun reset() {
        history.clear()
    }
}

/**
 * Shannon entropy of the state distribution.
 * H = -Σ p_i * log2(p_i)
 * Measures disorder/complexity of the lattice.
 */
class EntropyAnalysis : Analysis {
    override val displayName = "Entropy"
    override val description = "Shannon entropy of state distribution"
    override val category = AnalysisCategory.STATISTICAL

    private val history = mutableListOf<Double>()

    override fun analyze(lattice: Lattice, rule: Rule, generation: Long): List<AnalysisResult> {
        val total = lattice.cellCount
        if (total == 0) return emptyList()

        val counts = mutableMapOf<Int, Int>()
        for (cell in lattice.cells) {
            val s = cell.currentState.toInt()
            counts[s] = (counts[s] ?: 0) + 1
        }

        var entropy = 0.0
        for ((_, count) in counts) {
            if (count > 0) {
                val p = count.toDouble() / total
                entropy -= p * (ln(p) / LN2)
            }
        }

        history.add(entropy)
        if (history.size > MAX_HISTORY) history.removeAt(0)

        return listOf(
            AnalysisResult.SingleValue("Current Entropy", entropy, "bits"),
            AnalysisResult.TimeSeries("Entropy", history.toList())
        )
    }

    override fun reset() {
        history.clear()
    }
}

/**
 * Fraction of cells that changed state from previous generation.
 * Measures how dynamic/active the simulation is.
 */
class ActivityAnalysis : Analysis {
    override val displayName = "Activity"
    override val description = "Fraction of cells that changed state"
    override val category = AnalysisCategory.DYNAMICS

    private val history = mutableListOf<Double>()

    override fun analyze(lattice: Lattice, rule: Rule, generation: Long): List<AnalysisResult> {
        val total = lattice.cellCount
        if (total == 0) return emptyList()

        var changed = 0
        for (cell in lattice.cells) {
            val prev = cell.previousState
            if (prev != null && cell.currentState.toInt() != prev.toInt()) {
                changed++
            }
        }

        val activity = changed.toDouble() / total

        history.add(activity)
        if (history.size > MAX_HISTORY) history.removeAt(0)

        return listOf(
            AnalysisResult.SingleValue("Activity", activity),
            AnalysisResult.TimeSeries("Activity", history.toList())
        )
    }

    override fun reset() {
        history.clear()
    }
}
