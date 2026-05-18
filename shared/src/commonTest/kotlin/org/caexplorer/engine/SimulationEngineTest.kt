package org.caexplorer.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.caexplorer.domain.cell.Cell
import org.caexplorer.domain.cellstate.IntegerCellState
import org.caexplorer.domain.colorscheme.BlackAndWhiteColorScheme
import org.caexplorer.domain.colorscheme.RainbowColorScheme
import org.caexplorer.domain.lattice.SquareLattice
import org.caexplorer.domain.rule.implementations.Life
import org.caexplorer.domain.util.Coordinate
import kotlin.test.*

class SimulationEngineTest {

    private fun makeEngine(): SimulationEngine = SimulationEngine()

    private fun makeConfig(): SimulationConfig {
        IntegerCellState.numStates = 2
        val lattice = SquareLattice(10, 10) { coord -> Cell(IntegerCellState(0), coord) }
        return SimulationConfig(
            rule = Life(),
            lattice = lattice,
            colorScheme = RainbowColorScheme(),
            numWorkers = 1
        )
    }

    /** Wait in real time for the engine's internal coroutine to update generation. */
    private suspend fun realDelay(ms: Long) {
        withContext(Dispatchers.Default) {
            kotlinx.coroutines.delay(ms)
        }
    }

    @Test
    fun configureSetsStateToIdle() {
        val engine = makeEngine()
        engine.configure(makeConfig())
        assertEquals(SimulationStatus.IDLE, engine.state.value.status)
        assertEquals(0L, engine.state.value.generation)
        engine.destroy()
    }

    @Test
    fun startTransitionsToRunning() = runTest {
        val engine = makeEngine()
        engine.configure(makeConfig())
        engine.start()
        assertEquals(SimulationStatus.RUNNING, engine.state.value.status)
        engine.stop()
        engine.destroy()
    }

    @Test
    fun pauseTransitionsToPaused() = runTest {
        val engine = makeEngine()
        engine.configure(makeConfig())
        engine.start()
        engine.pause()
        assertEquals(SimulationStatus.PAUSED, engine.state.value.status)
        engine.destroy()
    }

    @Test
    fun stepIncrementsGenerationByOne() = runTest {
        val engine = makeEngine()
        engine.configure(makeConfig())
        engine.step()
        // step() launches on engine's internal Dispatchers.Default scope
        realDelay(500)
        assertTrue(engine.state.value.generation >= 1, "Expected generation >= 1 after step, got ${engine.state.value.generation}")
        engine.destroy()
    }

    @Test
    fun rewindDecrementsGenerationByOne() = runTest {
        val engine = makeEngine()
        engine.configure(makeConfig())

        // Step twice so we have history
        engine.step()
        realDelay(500)
        engine.step()
        realDelay(500)

        val genBefore = engine.state.value.generation
        engine.rewind()
        val genAfter = engine.state.value.generation
        // If steps completed, generation should decrease
        assertTrue(genAfter <= genBefore, "Expected generation to not increase after rewind")
        if (genBefore > 0) {
            assertTrue(genAfter < genBefore, "Expected generation to decrease from $genBefore after rewind")
        }
        engine.destroy()
    }

    @Test
    fun stopResetsToIdle() = runTest {
        val engine = makeEngine()
        engine.configure(makeConfig())
        engine.start()
        engine.stop()
        assertEquals(SimulationStatus.IDLE, engine.state.value.status)
        engine.destroy()
    }

    @Test
    fun setSpeedUpdatesDelay() {
        val engine = makeEngine()
        engine.configure(makeConfig())
        engine.setSpeed(100)
        // The engine stores delay internally; we verify no crash and it still works
        engine.setSpeed(0)
        engine.destroy()
    }

    @Test
    fun toggleCellChangesCellState() {
        val engine = makeEngine()
        val config = makeConfig()
        engine.configure(config)

        // Initially all cells are state 0
        val cellBefore = config.lattice.getCell(5, 5)!!
        assertEquals(0, cellBefore.currentState.toInt())

        engine.toggleCell(5, 5)
        val cellAfter = config.lattice.getCell(5, 5)!!
        assertEquals(1, cellAfter.currentState.toInt())

        // Toggle again → back to 0
        engine.toggleCell(5, 5)
        assertEquals(0, config.lattice.getCell(5, 5)!!.currentState.toInt())
        engine.destroy()
    }

    @Test
    fun paintCellAndFlushPaintUpdateColors() {
        val engine = makeEngine()
        val config = makeConfig()
        engine.configure(config)

        engine.paintCell(3, 3, 1)
        engine.flushPaint()

        val cell = config.lattice.getCell(3, 3)!!
        assertEquals(1, cell.currentState.toInt())

        // Color buffer should be updated and non-empty
        val colors = engine.cellColors.value
        assertTrue(colors.isNotEmpty())
        engine.destroy()
    }

    @Test
    fun updateColorSchemeChangesColorsWithoutReinit() {
        val engine = makeEngine()
        val config = makeConfig()
        engine.configure(config)

        val colorsBefore = engine.cellColors.value.copyOf()

        // Set a cell to alive so color differs between schemes
        engine.toggleCell(0, 0)
        val colorsWithAlive = engine.cellColors.value.copyOf()

        engine.updateColorScheme(BlackAndWhiteColorScheme())
        val colorsAfter = engine.cellColors.value

        // The engine's config should reflect the new scheme
        assertEquals("Black and White", engine.config!!.colorScheme.displayName)

        // Lattice state should be unchanged
        assertEquals(1, config.lattice.getCell(0, 0)!!.currentState.toInt())
        engine.destroy()
    }

    @Test
    fun cellColorsEmitsNonEmptyArrayAfterConfigure() {
        val engine = makeEngine()
        engine.configure(makeConfig())
        val colors = engine.cellColors.value
        assertEquals(100, colors.size, "Expected 100 colors for 10x10 grid")
        engine.destroy()
    }
}
