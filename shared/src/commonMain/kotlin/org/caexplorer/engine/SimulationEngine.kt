package org.caexplorer.engine

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.caexplorer.domain.cell.Cell
import org.caexplorer.domain.cellstate.CellState
import org.caexplorer.domain.colorscheme.ColorScheme
import org.caexplorer.domain.lattice.Lattice
import org.caexplorer.domain.rule.Rule
import kotlin.time.TimeSource

/**
 * The state of the simulation engine.
 */
enum class SimulationStatus {
    IDLE,
    RUNNING,
    PAUSED,
    STEPPING
}

/**
 * Observable state emitted by the simulation engine.
 */
data class SimulationState(
    val status: SimulationStatus = SimulationStatus.IDLE,
    val generation: Long = 0,
    val generationsPerSecond: Double = 0.0,
)

/** Default worker count. Platform-specific implementations can override. */
expect val DEFAULT_WORKERS: Int

/**
 * Configuration for a simulation run.
 */
data class SimulationConfig(
    val rule: Rule,
    val lattice: Lattice,
    val colorScheme: ColorScheme,
    val updateGraphicsEveryNSteps: Int = 1,
    val maxSteps: Long = Long.MAX_VALUE,
    val numWorkers: Int = DEFAULT_WORKERS
)

/**
 * Core simulation engine using Kotlin coroutines.
 *
 * Replaces Java's SwingWorker + CountDownLatch + ParallelProcessingWorker
 * with structured concurrency. Cell updates are partitioned across coroutines
 * on Dispatchers.Default (backed by a thread pool sized to CPU cores).
 *
 * Port of Java CAController.
 */
class SimulationEngine {

    private val _state = MutableStateFlow(SimulationState())
    val state: StateFlow<SimulationState> = _state.asStateFlow()

    private val _cellColors = MutableStateFlow(IntArray(0))
    /** The current RGB color for each cell, updated every N generations. */
    val cellColors: StateFlow<IntArray> = _cellColors.asStateFlow()

    private var simulationJob: Job? = null
    private var config: SimulationConfig? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Load a new simulation configuration. Resets the engine.
     */
    fun configure(config: SimulationConfig) {
        stop()
        this.config = config
        _state.value = SimulationState(status = SimulationStatus.IDLE, generation = 0)
        updateColorBuffer(config)
    }

    /**
     * Start continuous simulation.
     */
    fun start() {
        val cfg = config ?: return
        if (_state.value.status == SimulationStatus.RUNNING) return

        _state.value = _state.value.copy(status = SimulationStatus.RUNNING)
        simulationJob = scope.launch {
            runSimulation(cfg)
        }
    }

    /**
     * Pause the simulation (can be resumed).
     */
    fun pause() {
        if (_state.value.status != SimulationStatus.RUNNING) return
        _state.value = _state.value.copy(status = SimulationStatus.PAUSED)
        simulationJob?.cancel()
        simulationJob = null
    }

    /**
     * Resume a paused simulation.
     */
    fun resume() {
        if (_state.value.status != SimulationStatus.PAUSED) return
        start()
    }

    /**
     * Stop the simulation and reset to idle.
     */
    fun stop() {
        simulationJob?.cancel()
        simulationJob = null
        _state.value = _state.value.copy(status = SimulationStatus.IDLE)
    }

    /**
     * Advance exactly one generation.
     */
    fun step() {
        val cfg = config ?: return
        if (_state.value.status == SimulationStatus.RUNNING) return

        scope.launch {
            _state.value = _state.value.copy(status = SimulationStatus.STEPPING)
            incrementGeneration(cfg)
            updateColorBuffer(cfg)
            _state.value = _state.value.copy(
                status = SimulationStatus.PAUSED,
                generation = _state.value.generation + 1
            )
        }
    }

    /**
     * Rewind one generation (if cell history allows).
     */
    fun rewind() {
        val cfg = config ?: return
        if (_state.value.status == SimulationStatus.RUNNING) return

        val cells = cfg.lattice.cells
        var rewound = false
        for (cell in cells) {
            if (cell.rewind()) rewound = true
        }
        if (rewound) {
            updateColorBuffer(cfg)
            _state.value = _state.value.copy(
                generation = (_state.value.generation - 1).coerceAtLeast(0)
            )
        }
    }

    /**
     * Release resources.
     */
    fun destroy() {
        stop()
        scope.cancel()
    }

    // --- Internal simulation loop ---

    private suspend fun runSimulation(cfg: SimulationConfig) {
        var genCount = _state.value.generation
        val timeSource = TimeSource.Monotonic
        var lastMark = timeSource.markNow()
        var gensSinceLastMeasure = 0L

        while (isActive() && genCount < cfg.maxSteps) {
            incrementGeneration(cfg)
            genCount++
            gensSinceLastMeasure++

            // Update graphics every N steps
            if (genCount % cfg.updateGraphicsEveryNSteps == 0L) {
                updateColorBuffer(cfg)

                // Measure speed
                val elapsed = lastMark.elapsedNow().inWholeMilliseconds / 1000.0
                if (elapsed > 0.5) {
                    val gps = gensSinceLastMeasure / elapsed
                    _state.value = _state.value.copy(
                        generation = genCount,
                        generationsPerSecond = gps,
                        status = SimulationStatus.RUNNING
                    )
                    lastMark = timeSource.markNow()
                    gensSinceLastMeasure = 0
                }

                // Yield to allow UI updates
                yield()
            }
        }

        _state.value = _state.value.copy(
            status = SimulationStatus.PAUSED,
            generation = genCount
        )
    }

    /**
     * Advance all cells by one generation using parallel coroutines.
     * This is the performance-critical hot path.
     */
    private suspend fun incrementGeneration(cfg: SimulationConfig) {
        val cells = cfg.lattice.cells
        val rule = cfg.rule
        val lattice = cfg.lattice
        val numWorkers = cfg.numWorkers

        if (numWorkers <= 1 || cells.size < 100) {
            // Single-threaded path for small grids
            for (cell in cells) {
                val neighbors = lattice.getNeighbors(cell)
                val newState = rule.nextState(cell, neighbors)
                cell.addNewState(newState)
            }
        } else {
            // Parallel path: partition cells across coroutines
            val chunkSize = (cells.size + numWorkers - 1) / numWorkers
            coroutineScope {
                for (workerIdx in 0 until numWorkers) {
                    val from = workerIdx * chunkSize
                    val to = minOf(from + chunkSize, cells.size)
                    if (from >= to) continue

                    launch {
                        for (i in from until to) {
                            val cell = cells[i]
                            val neighbors = lattice.getNeighbors(cell)
                            val newState = rule.nextState(cell, neighbors)
                            cell.addNewState(newState)
                        }
                    }
                }
            }
        }
    }

    /**
     * Build the color buffer from current cell states.
     */
    private fun updateColorBuffer(cfg: SimulationConfig) {
        val cells = cfg.lattice.cells
        val scheme = cfg.colorScheme
        val numStates = (cfg.rule as? org.caexplorer.domain.rule.IntegerRule)?.numStates ?: 2

        val colors = IntArray(cells.size) { i ->
            val state = cells[i].currentState.toInt()
            val color = scheme.getColor(state, numStates)
            // Convert Compose Color to ARGB int
            val r = (color.red * 255).toInt()
            val g = (color.green * 255).toInt()
            val b = (color.blue * 255).toInt()
            (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        _cellColors.value = colors
    }
}

private suspend fun isActive(): Boolean = currentCoroutineContext().isActive
