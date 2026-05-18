package org.caexplorer.analysis

import org.caexplorer.domain.cell.Cell
import org.caexplorer.domain.cellstate.IntegerCellState
import org.caexplorer.domain.lattice.SquareLattice
import org.caexplorer.domain.rule.implementations.Life
import org.caexplorer.domain.util.Coordinate
import kotlin.math.abs
import kotlin.math.ln
import kotlin.test.*

class AnalysisTest {

    private fun makeGrid(
        width: Int,
        height: Int,
        initState: (Coordinate) -> Int = { 0 }
    ): SquareLattice {
        IntegerCellState.numStates = 2
        return SquareLattice(width, height) { coord ->
            Cell(IntegerCellState(initState(coord)), coord)
        }
    }

    // --- PopulationAnalysis ---

    @Test
    fun populationAnalysisCountsAllStates() {
        val lattice = makeGrid(5, 5) { coord ->
            if (coord.row == 0) 1 else 0
        }
        val analysis = PopulationAnalysis()
        val results = analysis.analyze(lattice, Life(), 0)

        // Should have a histogram
        val histogram = results.filterIsInstance<AnalysisResult.Histogram>().first()
        assertEquals("State Distribution", histogram.label)

        val state0Count = histogram.bins["State 0"]!!
        val state1Count = histogram.bins["State 1"]!!
        assertEquals(20.0, state0Count) // 5x5 - 5 alive = 20 dead
        assertEquals(5.0, state1Count)  // row 0 = 5 alive
    }

    // --- DensityAnalysis ---

    @Test
    fun densityAnalysisComputesCorrectFraction() {
        // 10 out of 25 cells alive
        val lattice = makeGrid(5, 5) { coord ->
            if (coord.row < 2) 1 else 0
        }
        val analysis = DensityAnalysis()
        val results = analysis.analyze(lattice, Life(), 0)

        val density = results.filterIsInstance<AnalysisResult.SingleValue>()
            .first { it.label == "Current Density" }
        assertEquals(0.4, density.value, 0.001) // 10/25
    }

    // --- EntropyAnalysis ---

    @Test
    fun entropyAnalysisZeroEntropyForUniformLattice() {
        // All cells same state → entropy = 0
        val lattice = makeGrid(5, 5) { 0 }
        val analysis = EntropyAnalysis()
        val results = analysis.analyze(lattice, Life(), 0)

        val entropy = results.filterIsInstance<AnalysisResult.SingleValue>()
            .first { it.label == "Current Entropy" }
        assertEquals(0.0, entropy.value, 0.001)
    }

    @Test
    fun entropyAnalysisMaxEntropyForEvenDistribution() {
        // For 2 states, max entropy is 1.0 bit (when 50/50 split)
        // Create a lattice where half are 0 and half are 1
        // Use a 4x4 grid = 16 cells, 8 alive, 8 dead
        val lattice = makeGrid(4, 4) { coord ->
            if (coord.row < 2) 1 else 0
        }
        val analysis = EntropyAnalysis()
        val results = analysis.analyze(lattice, Life(), 0)

        val entropy = results.filterIsInstance<AnalysisResult.SingleValue>()
            .first { it.label == "Current Entropy" }
        // Max Shannon entropy for 2 states = 1.0 bit
        assertEquals(1.0, entropy.value, 0.001)
    }

    // --- ActivityAnalysis ---

    @Test
    fun activityAnalysisDetectsChangesAfterStep() {
        val lattice = makeGrid(5, 5) { 0 }
        val rule = Life()
        val analysis = ActivityAnalysis()

        // Before any step, no previous state → activity = 0
        val resultsBefore = analysis.analyze(lattice, rule, 0)
        val actBefore = resultsBefore.filterIsInstance<AnalysisResult.SingleValue>()
            .first { it.label == "Activity" }
        assertEquals(0.0, actBefore.value, 0.001)

        // Manually evolve: set some cells, then add new states to simulate a step
        lattice.getCell(2, 2)!!.resetState(IntegerCellState(1))
        // Simulate a generation for all cells
        for (cell in lattice.cells) {
            val neighbors = lattice.getNeighbors(cell)
            val newState = rule.nextState(cell, neighbors)
            cell.addNewState(newState)
        }

        analysis.reset()
        val resultsAfter = analysis.analyze(lattice, rule, 1)
        val actAfter = resultsAfter.filterIsInstance<AnalysisResult.SingleValue>()
            .first { it.label == "Activity" }
        // The cell at (2,2) changed from 1→0 (died with 0 neighbors)
        assertTrue(actAfter.value > 0.0, "Expected activity > 0 after step with changes")
    }

    // --- ClusterAnalysis ---

    @Test
    fun clusterAnalysisFindsConnectedComponents() {
        // Create two separate clusters of alive cells
        val lattice = makeGrid(5, 5) { coord ->
            when {
                // Cluster 1: top-left 2x2 block
                coord.row < 2 && coord.col < 2 -> 1
                // Cluster 2: bottom-right single cell
                coord.row == 4 && coord.col == 4 -> 1
                else -> 0
            }
        }
        val analysis = ClusterAnalysis()
        val results = analysis.analyze(lattice, Life(), 0)

        val clusterCount = results.filterIsInstance<AnalysisResult.SingleValue>()
            .first { it.label == "Clusters" }
        assertEquals(2.0, clusterCount.value) // Two separate clusters

        val largest = results.filterIsInstance<AnalysisResult.SingleValue>()
            .first { it.label == "Largest" }
        assertEquals(4.0, largest.value) // The 2x2 block has 4 cells
    }

    // --- HammingDistanceAnalysis ---

    @Test
    fun hammingDistanceAnalysisMeasuresDistance() {
        val lattice = makeGrid(5, 5) { 0 }
        val rule = Life()
        val analysis = HammingDistanceAnalysis()

        // No previous state → distance = 0
        val resultsBefore = analysis.analyze(lattice, rule, 0)
        val distBefore = resultsBefore.filterIsInstance<AnalysisResult.SingleValue>()
            .first { it.label == "Hamming Distance" }
        assertEquals(0.0, distBefore.value)

        // Set up a blinker pattern and step
        lattice.getCell(2, 1)!!.resetState(IntegerCellState(1))
        lattice.getCell(2, 2)!!.resetState(IntegerCellState(1))
        lattice.getCell(2, 3)!!.resetState(IntegerCellState(1))

        // Step the lattice manually
        for (cell in lattice.cells) {
            val neighbors = lattice.getNeighbors(cell)
            val newState = rule.nextState(cell, neighbors)
            cell.addNewState(newState)
        }

        analysis.reset()
        val resultsAfter = analysis.analyze(lattice, rule, 1)
        val distAfter = resultsAfter.filterIsInstance<AnalysisResult.SingleValue>()
            .first { it.label == "Hamming Distance" }
        // A blinker oscillates: cells change state
        assertTrue(distAfter.value > 0.0, "Expected Hamming distance > 0 after blinker step")
    }

    // --- AnalysisRegistry ---

    @Test
    fun analysisRegistryContainsAllAnalyses() {
        val all = AnalysisRegistry.getAll()
        assertTrue(all.isNotEmpty(), "Registry should not be empty")

        val names = all.map { it.displayName }
        assertTrue("Population" in names, "Missing PopulationAnalysis")
        assertTrue("Density" in names, "Missing DensityAnalysis")
        assertTrue("Entropy" in names, "Missing EntropyAnalysis")
        assertTrue("Activity" in names, "Missing ActivityAnalysis")
        assertTrue("Clusters" in names, "Missing ClusterAnalysis")
        assertTrue("Hamming Distance" in names, "Missing HammingDistanceAnalysis")
        assertTrue("Mutual Information" in names, "Missing MutualInformationAnalysis")
        assertTrue("Center of Mass" in names, "Missing CenterOfMassAnalysis")
    }
}
