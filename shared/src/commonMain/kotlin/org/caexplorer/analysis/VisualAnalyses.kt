package org.caexplorer.analysis

import org.caexplorer.domain.lattice.Lattice
import org.caexplorer.domain.rule.Rule
import kotlin.math.sqrt

private const val MAX_HISTORY = 500

/**
 * Extracts horizontal (middle row) and vertical (middle column) slices
 * of cell states, providing 1D cross-sections of 2D patterns.
 */
class SliceAnalysis : Analysis {
    override val displayName = "Slice"
    override val description = "Horizontal and vertical cross-section slices"
    override val category = AnalysisCategory.VISUAL

    private val hSliceHistory = mutableListOf<List<Double>>()
    private val vSliceHistory = mutableListOf<List<Double>>()

    override fun analyze(lattice: Lattice, rule: Rule, generation: Long): List<AnalysisResult> {
        val width = lattice.width
        val height = lattice.height
        if (width == 0 || height == 0) return emptyList()

        val midRow = height / 2
        val midCol = width / 2

        // Horizontal slice: middle row
        val hSlice = mutableListOf<Double>()
        for (c in 0 until width) {
            val idx = midRow * width + c
            if (idx < lattice.cellCount) {
                hSlice.add(lattice.cells[idx].currentState.toInt().toDouble())
            }
        }

        // Vertical slice: middle column
        val vSlice = mutableListOf<Double>()
        for (r in 0 until height) {
            val idx = r * width + midCol
            if (idx < lattice.cellCount) {
                vSlice.add(lattice.cells[idx].currentState.toInt().toDouble())
            }
        }

        hSliceHistory.add(hSlice)
        if (hSliceHistory.size > MAX_HISTORY) hSliceHistory.removeAt(0)

        vSliceHistory.add(vSlice)
        if (vSliceHistory.size > MAX_HISTORY) vSliceHistory.removeAt(0)

        return listOf(
            AnalysisResult.TimeSeries("H-Slice (row $midRow)", hSlice),
            AnalysisResult.TimeSeries("V-Slice (col $midCol)", vSlice)
        )
    }

    override fun reset() {
        hSliceHistory.clear()
        vSliceHistory.clear()
    }
}

/**
 * Measures horizontal and vertical symmetry of the lattice.
 * Compares left half vs flipped right half and top half vs flipped bottom half.
 * Symmetry is the fraction of matching cells.
 */
class SymmetryAnalysis : Analysis {
    override val displayName = "Symmetry"
    override val description = "Horizontal and vertical symmetry measurement"
    override val category = AnalysisCategory.VISUAL

    private val hSymmetryHistory = mutableListOf<Double>()
    private val vSymmetryHistory = mutableListOf<Double>()

    override fun analyze(lattice: Lattice, rule: Rule, generation: Long): List<AnalysisResult> {
        val width = lattice.width
        val height = lattice.height
        if (width == 0 || height == 0) return emptyList()

        // Horizontal symmetry: compare left half vs flipped right half
        var hMatches = 0
        var hTotal = 0
        for (r in 0 until height) {
            for (c in 0 until width / 2) {
                val leftIdx = r * width + c
                val rightIdx = r * width + (width - 1 - c)
                if (leftIdx < lattice.cellCount && rightIdx < lattice.cellCount) {
                    hTotal++
                    if (lattice.cells[leftIdx].currentState.toInt() ==
                        lattice.cells[rightIdx].currentState.toInt()) {
                        hMatches++
                    }
                }
            }
        }

        // Vertical symmetry: compare top half vs flipped bottom half
        var vMatches = 0
        var vTotal = 0
        for (r in 0 until height / 2) {
            for (c in 0 until width) {
                val topIdx = r * width + c
                val bottomIdx = (height - 1 - r) * width + c
                if (topIdx < lattice.cellCount && bottomIdx < lattice.cellCount) {
                    vTotal++
                    if (lattice.cells[topIdx].currentState.toInt() ==
                        lattice.cells[bottomIdx].currentState.toInt()) {
                        vMatches++
                    }
                }
            }
        }

        val hSymmetry = if (hTotal > 0) hMatches.toDouble() / hTotal else 0.0
        val vSymmetry = if (vTotal > 0) vMatches.toDouble() / vTotal else 0.0

        hSymmetryHistory.add(hSymmetry)
        if (hSymmetryHistory.size > MAX_HISTORY) hSymmetryHistory.removeAt(0)

        vSymmetryHistory.add(vSymmetry)
        if (vSymmetryHistory.size > MAX_HISTORY) vSymmetryHistory.removeAt(0)

        return listOf(
            AnalysisResult.SingleValue("H-Symmetry", hSymmetry),
            AnalysisResult.SingleValue("V-Symmetry", vSymmetry),
            AnalysisResult.TimeSeries("H-Symmetry", hSymmetryHistory.toList()),
            AnalysisResult.TimeSeries("V-Symmetry", vSymmetryHistory.toList())
        )
    }

    override fun reset() {
        hSymmetryHistory.clear()
        vSymmetryHistory.clear()
    }
}

/**
 * Tracks center of mass velocity between generations.
 * Speed = sqrt(dx^2 + dy^2) where dx/dy are per-generation center-of-mass shifts.
 */
class SpeedAnalysis : Analysis {
    override val displayName = "Wave Speed"
    override val description = "Center of mass velocity between generations"
    override val category = AnalysisCategory.VISUAL

    private val speedHistory = mutableListOf<Double>()
    private var prevComX: Double? = null
    private var prevComY: Double? = null

    override fun analyze(lattice: Lattice, rule: Rule, generation: Long): List<AnalysisResult> {
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

        if (aliveCount == 0) {
            prevComX = null
            prevComY = null
            speedHistory.add(0.0)
            if (speedHistory.size > MAX_HISTORY) speedHistory.removeAt(0)
            return listOf(
                AnalysisResult.SingleValue("Speed", 0.0, "cells/gen"),
                AnalysisResult.TimeSeries("Speed", speedHistory.toList())
            )
        }

        val comX = sumX / aliveCount
        val comY = sumY / aliveCount

        val speed = if (prevComX != null && prevComY != null) {
            val dx = comX - prevComX!!
            val dy = comY - prevComY!!
            sqrt(dx * dx + dy * dy)
        } else {
            0.0
        }

        prevComX = comX
        prevComY = comY

        speedHistory.add(speed)
        if (speedHistory.size > MAX_HISTORY) speedHistory.removeAt(0)

        return listOf(
            AnalysisResult.SingleValue("Speed", speed, "cells/gen"),
            AnalysisResult.TimeSeries("Speed", speedHistory.toList())
        )
    }

    override fun reset() {
        speedHistory.clear()
        prevComX = null
        prevComY = null
    }
}
