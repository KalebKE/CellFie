package org.caexplorer.engine

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.caexplorer.domain.cell.Cell
import org.caexplorer.domain.cellstate.CellState
import org.caexplorer.domain.cellstate.IntegerCellState
import org.caexplorer.domain.colorscheme.ColorScheme
import org.caexplorer.domain.lattice.Lattice
import org.caexplorer.domain.rule.IntegerRule
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

    private val _cellStates = MutableStateFlow(IntArray(0))
    /** Raw integer state values for each cell (used by the 3D renderer). */
    val cellStates: StateFlow<IntArray> = _cellStates.asStateFlow()

    private val _delayMs = MutableStateFlow(0L)

    @Volatile
    private var activeColorScheme: ColorScheme? = null

    private var simulationJob: Job? = null
    private var _config: SimulationConfig? = null

    /** Current simulation configuration (read-only access for analysis). */
    val config: SimulationConfig? get() = _config

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Double-buffered color arrays: alternating references so StateFlow
    // always sees a new identity and emits updates to the UI.
    private var colorBufferA: IntArray = IntArray(0)
    private var colorBufferB: IntArray = IntArray(0)
    private var useBufferA = true

    // Double-buffered state arrays for the 3D renderer
    private var stateBufferA: IntArray = IntArray(0)
    private var stateBufferB: IntArray = IntArray(0)
    private var useStateBufferA = true

    /** Whether the current lattice is three-dimensional. */
    val is3D: Boolean get() = _config?.lattice?.isThreeDimensional == true

    // --- Artistic render effects ---
    @Volatile var trailEnabled: Boolean = false
    @Volatile var trailDecay: Float = 0.92f

    @Volatile var bloomEnabled: Boolean = false
    @Volatile var bloomRadius: Int = 3
    @Volatile var bloomIntensity: Float = 0.6f

    @Volatile var smoothEnabled: Boolean = false

    // Trail intensity buffer — persists across generations
    private var trailBuffer: FloatArray = FloatArray(0)

    // Batch painting support
    @Volatile
    private var pendingColorUpdate = false

    /**
     * Load a new simulation configuration. Resets the engine.
     */
    fun configure(config: SimulationConfig) {
        stop()
        this._config = config
        activeColorScheme = config.colorScheme
        trailBuffer = FloatArray(0) // reset trails on new config
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

    /**
     * Set the simulation speed delay in milliseconds.
     * 0 = maximum speed, higher = slower.
     */
    fun setSpeed(delayMs: Long) {
        _delayMs.value = delayMs.coerceAtLeast(0)
    }

    /**
     * Update the color scheme used for rendering without reinitializing.
     */
    fun updateColorScheme(scheme: ColorScheme) {
        activeColorScheme = scheme
        val cfg = config ?: return
        this._config = cfg.copy(colorScheme = scheme)
        updateColorBuffer(cfg)
    }

    /**
     * Reset the engine state (stop + clear generation counter).
     * Re-initialization of cells is handled by the caller.
     */
    fun reset() {
        stop()
        _state.value = SimulationState(status = SimulationStatus.IDLE, generation = 0)
    }

    // --- Cell drawing / painting ---

    /**
     * Toggle a cell between empty and max state.
     */
    fun toggleCell(row: Int, col: Int) {
        val cfg = config ?: return
        if (row < 0 || row >= cfg.lattice.height || col < 0 || col >= cfg.lattice.width) return
        val index = row * cfg.lattice.width + col
        val cell = cfg.lattice.cells.getOrNull(index) ?: return
        val numStates = (cfg.rule as? IntegerRule)?.numStates ?: 2
        val current = cell.currentState.toInt()
        val newState = if (current == 0) numStates - 1 else 0
        cell.resetState(IntegerCellState(newState))
        updateColorBuffer(cfg)
    }

    /**
     * Paint a cell to a specific state. Batched — call [flushPaint] to update colors.
     */
    fun paintCell(row: Int, col: Int, state: Int) {
        val cfg = config ?: return
        if (row < 0 || row >= cfg.lattice.height || col < 0 || col >= cfg.lattice.width) return
        val index = row * cfg.lattice.width + col
        val cell = cfg.lattice.cells.getOrNull(index) ?: return
        cell.resetState(IntegerCellState(state))
        pendingColorUpdate = true
    }

    /**
     * Flush any pending paint updates to the color buffer.
     */
    fun flushPaint() {
        if (pendingColorUpdate) {
            val cfg = config ?: return
            updateColorBuffer(cfg)
            pendingColorUpdate = false
        }
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

            // Speed control delay
            val speedDelay = _delayMs.value
            if (speedDelay > 0) delay(speedDelay)

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
     * Applies artistic render effects: trail decay, bloom/glow.
     * Also emits raw state values for the 3D renderer.
     */
    private fun updateColorBuffer(cfg: SimulationConfig) {
        val cells = cfg.lattice.cells
        val scheme = activeColorScheme ?: cfg.colorScheme
        val numStates = (cfg.rule as? IntegerRule)?.numStates ?: 2
        val cellCount = cells.size
        val width = cfg.lattice.width
        val height = cfg.lattice.height

        if (colorBufferA.size != cellCount) {
            colorBufferA = IntArray(cellCount)
            colorBufferB = IntArray(cellCount)
            stateBufferA = IntArray(cellCount)
            stateBufferB = IntArray(cellCount)
        }

        // --- Trail decay: maintain a persistent intensity per cell ---
        val useTrail = trailEnabled
        if (useTrail && trailBuffer.size != cellCount) {
            trailBuffer = FloatArray(cellCount)
        }
        val decay = trailDecay

        val colorBuf = if (useBufferA) colorBufferA else colorBufferB
        val stateBuf = if (useStateBufferA) stateBufferA else stateBufferB

        for (i in cells.indices) {
            val state = cells[i].currentState.toInt()
            stateBuf[i] = state

            if (useTrail) {
                // Active cells → full intensity; dead cells decay
                val liveIntensity = if (numStates <= 2) {
                    if (state > 0) 1f else 0f
                } else {
                    state.toFloat() / (numStates - 1).toFloat()
                }

                if (liveIntensity > trailBuffer[i]) {
                    trailBuffer[i] = liveIntensity
                } else {
                    trailBuffer[i] *= decay
                    if (trailBuffer[i] < 0.005f) trailBuffer[i] = 0f
                }

                val intensity = trailBuffer[i]
                val color = scheme.getColor(intensity.toDouble())
                val r = (color.red * 255).toInt()
                val g = (color.green * 255).toInt()
                val b = (color.blue * 255).toInt()
                colorBuf[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            } else {
                val color = scheme.getColor(state, numStates)
                val r = (color.red * 255).toInt()
                val g = (color.green * 255).toInt()
                val b = (color.blue * 255).toInt()
                colorBuf[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        // --- Bloom/glow post-processing ---
        if (bloomEnabled && width > 0 && height > 0 && !cfg.lattice.isThreeDimensional) {
            applyBloom(colorBuf, width, height, bloomRadius, bloomIntensity)
        }

        _cellColors.value = colorBuf
        _cellStates.value = stateBuf
        useBufferA = !useBufferA
        useStateBufferA = !useStateBufferA
    }

    /**
     * Apply a box-blur bloom effect to the color buffer.
     * Blurs the image, then additively composites the blur on top.
     */
    private fun applyBloom(buf: IntArray, width: Int, height: Int, radius: Int, intensity: Float) {
        val size = width * height
        if (buf.size < size) return
        val blurR = FloatArray(size)
        val blurG = FloatArray(size)
        val blurB = FloatArray(size)

        // Horizontal blur pass
        val tempR = FloatArray(size)
        val tempG = FloatArray(size)
        val tempB = FloatArray(size)
        val kernelSize = (radius * 2 + 1).toFloat()

        for (row in 0 until height) {
            val rowOff = row * width
            for (col in 0 until width) {
                var rSum = 0f; var gSum = 0f; var bSum = 0f
                for (dx in -radius..radius) {
                    val c = (col + dx).coerceIn(0, width - 1)
                    val pixel = buf[rowOff + c]
                    rSum += ((pixel shr 16) and 0xFF).toFloat()
                    gSum += ((pixel shr 8) and 0xFF).toFloat()
                    bSum += (pixel and 0xFF).toFloat()
                }
                val idx = rowOff + col
                tempR[idx] = rSum / kernelSize
                tempG[idx] = gSum / kernelSize
                tempB[idx] = bSum / kernelSize
            }
        }

        // Vertical blur pass
        for (col in 0 until width) {
            for (row in 0 until height) {
                var rSum = 0f; var gSum = 0f; var bSum = 0f
                for (dy in -radius..radius) {
                    val r = (row + dy).coerceIn(0, height - 1)
                    val idx = r * width + col
                    rSum += tempR[idx]
                    gSum += tempG[idx]
                    bSum += tempB[idx]
                }
                val idx = row * width + col
                blurR[idx] = rSum / kernelSize
                blurG[idx] = gSum / kernelSize
                blurB[idx] = bSum / kernelSize
            }
        }

        // Additive composite: original + blur * intensity
        for (i in 0 until size) {
            val origR = ((buf[i] shr 16) and 0xFF).toFloat()
            val origG = ((buf[i] shr 8) and 0xFF).toFloat()
            val origB = (buf[i] and 0xFF).toFloat()
            val r = (origR + blurR[i] * intensity).coerceAtMost(255f).toInt()
            val g = (origG + blurG[i] * intensity).coerceAtMost(255f).toInt()
            val b = (origB + blurB[i] * intensity).coerceAtMost(255f).toInt()
            buf[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
    }
}

private suspend fun isActive(): Boolean = currentCoroutineContext().isActive
