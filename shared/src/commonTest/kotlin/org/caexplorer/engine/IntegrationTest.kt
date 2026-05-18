package org.caexplorer.engine

import kotlin.test.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.caexplorer.domain.cell.Cell
import org.caexplorer.domain.cellstate.IntegerCellState
import org.caexplorer.domain.colorscheme.RainbowColorScheme
import org.caexplorer.domain.lattice.LatticeType
import org.caexplorer.domain.lattice.SquareLattice
import org.caexplorer.domain.rule.RuleRegistry
import org.caexplorer.domain.rule.IntegerRule
import org.caexplorer.domain.rule.RealRule
import org.caexplorer.domain.rule.implementations.*
import org.caexplorer.domain.util.Coordinate
import org.caexplorer.data.SimulationFileData
import org.caexplorer.data.serializeSimulation
import org.caexplorer.data.deserializeSimulation

class IntegrationTest {

    /** Apply one generation of a rule across all cells in a lattice. */
    private fun advanceGeneration(config: SimulationConfig) {
        val cells = config.lattice.cells
        val rule = config.rule
        val lattice = config.lattice

        // Compute all new states first
        val newStates = Array(cells.size) { i ->
            val cell = cells[i]
            val neighbors = lattice.getNeighbors(cell)
            rule.nextState(cell, neighbors)
        }
        // Apply all new states
        for (i in cells.indices) {
            cells[i].addNewState(newStates[i])
        }
    }

    private fun createLifeGrid(
        width: Int, height: Int,
        initState: (Coordinate) -> Int
    ): SimulationConfig {
        IntegerCellState.numStates = 2
        val lattice = SquareLattice(width, height) { coord ->
            Cell(IntegerCellState(initState(coord)), coord)
        }
        return SimulationConfig(
            rule = Life(),
            lattice = lattice,
            colorScheme = RainbowColorScheme(),
            numWorkers = 1
        )
    }

    private fun cellState(config: SimulationConfig, row: Int, col: Int): Int {
        return config.lattice.getCell(row, col)!!.currentState.toInt()
    }

    // -----------------------------------------------------------------------
    // Test 1: Game of Life Blinker
    // -----------------------------------------------------------------------
    @Test
    fun gameOfLifeBlinkerOscillates() {
        // Blinker: 3 horizontal cells in center of 5x5 grid
        // . . . . .     . . X . .     . . . . .
        // . . . . .     . . X . .     . . . . .
        // . X X X .  →  . . X . .  →  . X X X .
        // . . . . .     . . . . .     . . . . .
        // . . . . .     . . . . .     . . . . .
        val blinkerCells = setOf(Coordinate(2, 1), Coordinate(2, 2), Coordinate(2, 3))
        val config = createLifeGrid(5, 5) { coord ->
            if (coord in blinkerCells) 1 else 0
        }

        // After 1 generation: should become vertical
        advanceGeneration(config)
        assertEquals(1, cellState(config, 1, 2), "Blinker gen1: (1,2) should be alive")
        assertEquals(1, cellState(config, 2, 2), "Blinker gen1: (2,2) should be alive")
        assertEquals(1, cellState(config, 3, 2), "Blinker gen1: (3,2) should be alive")
        assertEquals(0, cellState(config, 2, 1), "Blinker gen1: (2,1) should be dead")
        assertEquals(0, cellState(config, 2, 3), "Blinker gen1: (2,3) should be dead")

        // After 2nd generation: should be horizontal again
        advanceGeneration(config)
        assertEquals(1, cellState(config, 2, 1), "Blinker gen2: (2,1) should be alive")
        assertEquals(1, cellState(config, 2, 2), "Blinker gen2: (2,2) should be alive")
        assertEquals(1, cellState(config, 2, 3), "Blinker gen2: (2,3) should be alive")
        assertEquals(0, cellState(config, 1, 2), "Blinker gen2: (1,2) should be dead")
        assertEquals(0, cellState(config, 3, 2), "Blinker gen2: (3,2) should be dead")
    }

    // -----------------------------------------------------------------------
    // Test 2: Game of Life Block (still life)
    // -----------------------------------------------------------------------
    @Test
    fun gameOfLifeBlockIsStillLife() {
        // 2x2 block in center of 6x6 grid — should never change
        val blockCells = setOf(
            Coordinate(2, 2), Coordinate(2, 3),
            Coordinate(3, 2), Coordinate(3, 3)
        )
        val config = createLifeGrid(6, 6) { coord ->
            if (coord in blockCells) 1 else 0
        }

        // Run 10 generations
        repeat(10) { advanceGeneration(config) }

        // Verify block is unchanged
        for (row in 0 until 6) {
            for (col in 0 until 6) {
                val expected = if (Coordinate(row, col) in blockCells) 1 else 0
                assertEquals(
                    expected, cellState(config, row, col),
                    "Block should be stable at ($row,$col) after 10 gens"
                )
            }
        }
    }

    // -----------------------------------------------------------------------
    // Test 3: Wolfram Rule 30 deterministic
    // -----------------------------------------------------------------------
    @Test
    fun wolframRule30ProducesExpectedPattern() {
        // Use a 7x7 Moore grid to simulate rule 30 on the "top row" approach
        // Rule 30 lookup: index = (left<<2)|(center<<1)|right → bit of rule number 30
        // 30 in binary = 00011110
        // index: 0→0, 1→1, 2→1, 3→1, 4→1, 5→0, 6→0, 7→0
        //
        // We test by creating a single row with center=1, applying rule 30 manually
        val rule = WolframRule(30)
        IntegerCellState.numStates = 2

        // Simulate 1D: 7 cells, center (index 3) is 1
        // Initial: 0 0 0 1 0 0 0
        // For each cell, neighbors[0] = left, neighbors[1] = right
        // After 1 gen:
        //   cell 0: left=cell6=0, center=0, right=cell1=0 → index 0 → 0
        //   cell 1: left=cell0=0, center=0, right=cell2=0 → index 0 → 0
        //   cell 2: left=cell1=0, center=0, right=cell3=1 → index 1 → 1
        //   cell 3: left=cell2=0, center=1, right=cell4=0 → index 2 → 1
        //   cell 4: left=cell3=1, center=0, right=cell5=0 → index 4 → 1
        //   cell 5: left=cell4=0, center=0, right=cell6=0 → index 0 → 0
        //   cell 6: left=cell5=0, center=0, right=cell0=0 → index 0 → 0
        // Expected after 1 gen: 0 0 1 1 1 0 0

        // Create a 1D lattice using Standard1DLattice
        val lattice = org.caexplorer.domain.lattice.Standard1DLattice(7) { coord ->
            Cell(IntegerCellState(if (coord.col == 3) 1 else 0), coord)
        }

        // Apply rule 30 one generation
        val cells = lattice.cells
        val newStates = Array(cells.size) { i ->
            rule.nextState(cells[i], lattice.getNeighbors(cells[i]))
        }
        for (i in cells.indices) {
            cells[i].addNewState(newStates[i])
        }

        // Verify expected pattern: 0 0 1 1 1 0 0
        val expected = intArrayOf(0, 0, 1, 1, 1, 0, 0)
        for (i in expected.indices) {
            assertEquals(expected[i], cells[i].currentState.toInt(), "Rule 30 gen1 cell $i")
        }
    }

    // -----------------------------------------------------------------------
    // Test 4: SimulationFactory creates valid configs
    // -----------------------------------------------------------------------
    @Test
    fun simulationFactoryCreatesValidConfigs() {
        // Life-like rule
        val lifeConfig = SimulationFactory.createConfig(
            rule = Life(), width = 20, height = 15, numWorkers = 1
        )
        assertEquals(20, lifeConfig.lattice.width)
        assertEquals(15, lifeConfig.lattice.height)
        assertEquals(20 * 15, lifeConfig.lattice.cells.size)

        // Multi-state integer rule
        val cyclicConfig = SimulationFactory.createConfig(
            rule = CyclicCA(), width = 10, height = 10, numWorkers = 1
        )
        assertEquals(100, cyclicConfig.lattice.cells.size)

        // Real rule
        val realConfig = SimulationFactory.createConfig(
            rule = ContinuousCA.DEFAULT, width = 8, height = 8, numWorkers = 1
        )
        assertEquals(64, realConfig.lattice.cells.size)
    }

    // -----------------------------------------------------------------------
    // Test 5: SimulationFactory center seed
    // -----------------------------------------------------------------------
    @Test
    fun simulationFactoryCenterSeedOnlyCenterIsNonZero() {
        val config = SimulationFactory.createCenterSeedConfig(
            rule = Life(), width = 11, height = 11, numWorkers = 1
        )
        val lattice = config.lattice

        var nonZeroCount = 0
        for (cell in lattice.cells) {
            if (cell.currentState.toInt() != 0) {
                nonZeroCount++
                assertEquals(5, cell.coordinate.row, "Non-zero cell should be at center row")
                assertEquals(5, cell.coordinate.col, "Non-zero cell should be at center col")
            }
        }
        assertEquals(1, nonZeroCount, "Only center cell should be non-zero")
    }

    // -----------------------------------------------------------------------
    // Test 6: SimulationFactory from states
    // -----------------------------------------------------------------------
    @Test
    fun simulationFactoryFromStatesMatchesInput() {
        val states = intArrayOf(0, 1, 0, 1, 1, 1, 0, 1, 0)
        val config = SimulationFactory.createFromStates(
            rule = Life(), width = 3, height = 3, states = states, numWorkers = 1
        )

        for (row in 0 until 3) {
            for (col in 0 until 3) {
                val idx = row * 3 + col
                assertEquals(
                    states[idx], cellState(config, row, col),
                    "State at ($row,$col) should match input"
                )
            }
        }
    }

    // -----------------------------------------------------------------------
    // Test 7: All featured rules instantiate without error
    // -----------------------------------------------------------------------
    @Test
    fun allFeaturedRulesInstantiateAndRunOneStep() {
        val rules = RuleRegistry.getFeaturedRules()
        assertTrue(rules.isNotEmpty(), "Should have featured rules")

        for (rule in rules) {
            val config = SimulationFactory.createConfig(
                rule = rule,
                width = 5,
                height = 5,
                density = 0.5,
                numWorkers = 1
            )
            // Run 1 generation — should not throw
            advanceGeneration(config)
        }
    }

    // -----------------------------------------------------------------------
    // Test 8: File round-trip preserves simulation state
    // -----------------------------------------------------------------------
    @Test
    fun fileRoundTripPreservesSimulationState() {
        val config = createLifeGrid(10, 10) { coord ->
            if ((coord.row + coord.col) % 3 == 0) 1 else 0
        }

        // Run 5 generations
        repeat(5) { advanceGeneration(config) }

        // Extract states
        val cells = config.lattice.cells
        val stateArray = IntArray(cells.size) { cells[it].currentState.toInt() }

        // Serialize
        val fileData = SimulationFileData(
            ruleName = "Life",
            latticeType = "Square (Moore)",
            width = 10,
            height = 10,
            cellStates = stateArray,
            colorSchemeIndex = 0,
            generation = 5
        )
        val serialized = serializeSimulation(fileData)

        // Deserialize
        val restored = deserializeSimulation(serialized)
        assertNotNull(restored)

        // Create new simulation from restored data
        val restoredConfig = SimulationFactory.createFromStates(
            rule = Life(),
            width = restored.width,
            height = restored.height,
            states = restored.cellStates,
            numWorkers = 1
        )

        // Verify all cell states match
        for (i in cells.indices) {
            assertEquals(
                stateArray[i],
                restoredConfig.lattice.cells[i].currentState.toInt(),
                "Cell $i state mismatch after round-trip"
            )
        }
    }

    // -----------------------------------------------------------------------
    // Test 9: Color buffer matches cell count
    // -----------------------------------------------------------------------
    @Test
    fun colorBufferMatchesCellCount() {
        val engine = SimulationEngine()
        val config = SimulationFactory.createConfig(
            rule = Life(), width = 15, height = 20, numWorkers = 1
        )
        engine.configure(config)

        val colors = engine.cellColors.value
        assertEquals(15 * 20, colors.size, "Color buffer should equal width * height")
        engine.destroy()
    }

    // -----------------------------------------------------------------------
    // Test 10: Multiple generation run
    // -----------------------------------------------------------------------
    @Test
    fun multipleGenerationRunCompletes() = runTest {
        val config = SimulationFactory.createConfig(
            rule = Life(), width = 50, height = 50, density = 0.25, numWorkers = 1
        )

        // Run 100 generations synchronously
        repeat(100) { advanceGeneration(config) }

        // Verify we didn't crash and cells are still valid integers
        for (cell in config.lattice.cells) {
            val state = cell.currentState.toInt()
            assertTrue(state in 0..1, "Life cell state should be 0 or 1, got $state")
        }
    }
}
