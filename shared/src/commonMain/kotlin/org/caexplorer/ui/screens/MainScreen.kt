package org.caexplorer.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.caexplorer.analysis.AnalysisRegistry
import org.caexplorer.analysis.AnalysisResult
import org.caexplorer.data.AppSettings
import org.caexplorer.data.SettingsKeys
import org.caexplorer.data.SimulationFileData
import org.caexplorer.data.loadSimulationFile
import org.caexplorer.data.saveSimulationFile
import org.caexplorer.domain.cell.Cell
import org.caexplorer.domain.cellstate.IntegerCellState
import org.caexplorer.domain.cellstate.RealValuedState
import org.caexplorer.domain.colorscheme.ALL_COLOR_SCHEMES
import org.caexplorer.domain.lattice.LatticeType
import org.caexplorer.domain.lattice.createLattice
import org.caexplorer.domain.rule.*
import org.caexplorer.engine.*
import org.caexplorer.ui.AppActions
import org.caexplorer.ui.components.AboutDialog
import org.caexplorer.ui.components.AnalysisDashboard
import org.caexplorer.ui.components.ConfigPanel
import org.caexplorer.ui.components.HelpDialog
import org.caexplorer.ui.components.KeyboardShortcutsSheet
import org.caexplorer.ui.components.RulePickerSheet
import org.caexplorer.ui.components.SimulationCanvas
import org.caexplorer.ui.components.VoxelCanvas
import org.caexplorer.ui.util.exportImage
import org.caexplorer.ui.util.GifRecorder
import org.caexplorer.ui.theme.AppPalette
import kotlin.math.min
import kotlin.random.Random

/**
 * Initialization patterns for the simulation grid.
 */
enum class InitPattern(val displayName: String) {
    AUTO("Auto (rule-based)"),
    RANDOM_25("Random 25%"),
    RANDOM_50("Random 50%"),
    CENTER_SEED("Center Seed"),
    GRADIENT("Gradient"),
    CHECKERBOARD("Checkerboard"),
    DISK("Disk"),
    RING("Ring"),
    RANDOM_SYMMETRIC("Symmetric Random"),
    CROSS("Cross"),
    DIAGONAL("Diagonal Stripes")
}

/**
 * Main simulation screen with the CA canvas, Material 3 controls,
 * keyboard shortcuts, speed control, and theme toggling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    selectedPalette: AppPalette = AppPalette.FIRE,
    onPaletteChanged: (AppPalette) -> Unit = {}
) {
    // Engine
    val engine = remember { SimulationEngine() }
    val simState by engine.state.collectAsState()
    val cellColors by engine.cellColors.collectAsState()
    val cellStates by engine.cellStates.collectAsState()

    // Grid configuration — load from settings
    var gridWidth by remember { mutableStateOf(AppSettings.getInt(SettingsKeys.GRID_WIDTH, 200)) }
    var gridHeight by remember { mutableStateOf(AppSettings.getInt(SettingsKeys.GRID_HEIGHT, 200)) }
    var gridDepth by remember { mutableStateOf(AppSettings.getInt(SettingsKeys.GRID_DEPTH, 20)) }
    var gridVisible by remember { mutableStateOf(false) }
    var showConfig by remember { mutableStateOf(false) }
    var showAnalysis by remember { mutableStateOf(false) }
    var analysisResults by remember { mutableStateOf<Map<String, List<AnalysisResult>>>(emptyMap()) }
    val analyses = remember { AnalysisRegistry.getAll() }

    // Speed control: index into speedSteps
    val speedSteps = remember { listOf(0L, 10L, 25L, 50L, 100L, 200L, 500L, 1000L) }
    var speedIndex by remember { mutableStateOf(AppSettings.getInt(SettingsKeys.SPEED_INDEX, 0).coerceIn(0, 7)) }
    val simulationDelay = speedSteps[speedIndex]

    // Color schemes
    val colorSchemes = remember { ALL_COLOR_SCHEMES }
    var selectedColorSchemeIndex by remember {
        mutableStateOf(AppSettings.getInt(SettingsKeys.LAST_COLOR_SCHEME, 0).coerceIn(0, ALL_COLOR_SCHEMES.lastIndex))
    }

    // Rule selection
    val rules = remember { RuleRegistry.getFeaturedRules() }
    var selectedRuleIndex by remember {
        mutableStateOf(AppSettings.getInt(SettingsKeys.LAST_RULE_INDEX, 0).coerceIn(0, RuleRegistry.getFeaturedRules().lastIndex))
    }

    // Current rule instance (may differ from rules[selectedRuleIndex] if properties were changed)
    var currentRule by remember { mutableStateOf<Rule?>(rules.getOrNull(selectedRuleIndex)) }

    // Init pattern
    var initPattern by remember { mutableStateOf(InitPattern.AUTO) }

    // Lattice type — load from settings
    var selectedLatticeType by remember {
        val saved = AppSettings.getString(SettingsKeys.LATTICE_TYPE, LatticeType.SQUARE_MOORE.name)
        mutableStateOf(
            try { LatticeType.valueOf(saved) } catch (_: Exception) { LatticeType.SQUARE_MOORE }
        )
    }

    // Reset key — incrementing triggers re-initialization
    var resetKey by remember { mutableStateOf(0) }

    // Fit-to-window trigger for canvas zoom reset
    var fitToWindowTrigger by remember { mutableStateOf(0) }

    // Draw mode
    var drawMode by remember { mutableStateOf(false) }
    // Track status before entering draw mode so we can restore it
    var statusBeforeDrawMode by remember { mutableStateOf<SimulationStatus?>(null) }

    // Artistic render effects
    var trailEnabled by remember { mutableStateOf(false) }
    var trailDecay by remember { mutableStateOf(0.92f) }
    var bloomEnabled by remember { mutableStateOf(false) }
    var bloomIntensity by remember { mutableStateOf(0.6f) }
    var smoothEnabled by remember { mutableStateOf(false) }

    // 3D opacity controls
    var voxelOpacity by remember { mutableStateOf(0.8f) }
    var depthFadeEnabled by remember { mutableStateOf(false) }
    var depthFadeReversed by remember { mutableStateOf(false) }
    var layerMin by remember { mutableStateOf(0) }
    var layerMax by remember { mutableStateOf(Int.MAX_VALUE) }

    // GIF recording
    val gifRecorder = remember { GifRecorder() }
    var gifRecording by remember { mutableStateOf(false) }
    var gifFrameCount by remember { mutableStateOf(0) }
    val maxGifFrames = 500

    // Dialog states
    var showAbout by remember { mutableStateOf(false) }
    var showKeyboardHelp by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }

    // Canvas fade animation for rule switches
    var canvasFadeTrigger by remember { mutableStateOf(0) }
    val canvasAlpha by animateFloatAsState(
        targetValue = if (canvasFadeTrigger % 2 == 0) 1f else 0.3f,
        animationSpec = tween(durationMillis = 150),
        label = "canvas-fade"
    )

    // Propagate speed changes to engine
    LaunchedEffect(simulationDelay) {
        engine.setSpeed(simulationDelay)
    }

    // Propagate color scheme changes (without reinitializing simulation)
    LaunchedEffect(selectedColorSchemeIndex) {
        engine.updateColorScheme(colorSchemes[selectedColorSchemeIndex])
    }

    // Propagate render effect changes to engine
    LaunchedEffect(trailEnabled, trailDecay, bloomEnabled, bloomIntensity, smoothEnabled) {
        engine.trailEnabled = trailEnabled
        engine.trailDecay = trailDecay
        engine.bloomEnabled = bloomEnabled
        engine.bloomIntensity = bloomIntensity
        engine.smoothEnabled = smoothEnabled
    }

    // Run analyses when panel is visible and generation changes
    LaunchedEffect(showAnalysis, simState.generation) {
        if (!showAnalysis) return@LaunchedEffect
        val config = engine.config ?: return@LaunchedEffect
        val gen = simState.generation
        withContext(Dispatchers.Default) {
            val results = linkedMapOf<String, List<AnalysisResult>>()
            for (analysis in analyses) {
                results[analysis.displayName] = analysis.analyze(config.lattice, config.rule, gen)
            }
            analysisResults = results
        }
    }

    // Reset analyses on simulation reinit
    LaunchedEffect(resetKey) {
        analyses.forEach { it.reset() }
        analysisResults = emptyMap()
    }

    // Capture GIF frames when recording and generation changes
    LaunchedEffect(gifRecording, simState.generation) {
        if (!gifRecording) return@LaunchedEffect
        if (cellColors.isNotEmpty()) {
            gifRecorder.addFrame(cellColors, gridWidth, gridHeight)
            gifFrameCount = gifRecorder.frameCount
            if (gifRecorder.frameCount >= maxGifFrames) {
                gifRecording = false
                gifRecorder.stopAndSave()
                gifFrameCount = 0
            }
        }
    }

    // Persist settings on changes
    LaunchedEffect(gridWidth, gridHeight, gridDepth) {
        AppSettings.putInt(SettingsKeys.GRID_WIDTH, gridWidth)
        AppSettings.putInt(SettingsKeys.GRID_HEIGHT, gridHeight)
        AppSettings.putInt(SettingsKeys.GRID_DEPTH, gridDepth)
    }
    LaunchedEffect(selectedRuleIndex) {
        AppSettings.putInt(SettingsKeys.LAST_RULE_INDEX, selectedRuleIndex)
        currentRule = rules.getOrNull(selectedRuleIndex)
        // Trigger canvas fade on rule switch
        canvasFadeTrigger++
    }
    LaunchedEffect(canvasFadeTrigger) {
        if (canvasFadeTrigger % 2 != 0) {
            kotlinx.coroutines.delay(150)
            canvasFadeTrigger++
        }
    }
    LaunchedEffect(selectedColorSchemeIndex) {
        AppSettings.putInt(SettingsKeys.LAST_COLOR_SCHEME, selectedColorSchemeIndex)
    }
    LaunchedEffect(speedIndex) {
        AppSettings.putInt(SettingsKeys.SPEED_INDEX, speedIndex)
    }
    LaunchedEffect(selectedLatticeType) {
        AppSettings.putString(SettingsKeys.LATTICE_TYPE, selectedLatticeType.name)
        // Auto-reduce grid size when switching to 3D to avoid excessive cell count
        if (selectedLatticeType.is3D) {
            if (gridWidth > 50) gridWidth = 30
            if (gridHeight > 50) gridHeight = 30
        }
    }
    // Initialize simulation on rule/grid/reset/lattice changes
    LaunchedEffect(selectedRuleIndex, gridWidth, gridHeight, gridDepth, resetKey, selectedLatticeType) {
        // Reset layer slice to full range when grid changes
        layerMin = 0
        layerMax = gridDepth - 1

        val rule = currentRule ?: rules.getOrNull(selectedRuleIndex) ?: return@LaunchedEffect
        val numStates = (rule as? IntegerRule)?.numStates ?: 2
        IntegerCellState.numStates = numStates

        // Determine density based on init pattern and rule type
        val density = when (initPattern) {
            InitPattern.AUTO -> when {
                rule is BinaryRule && rule.category == RuleCategory.LIFE_LIKE -> 0.25
                rule is BinaryRule -> 0.30
                rule is IntegerRule && numStates > 2 -> 0.50
                else -> 0.25
            }
            InitPattern.RANDOM_25 -> 0.25
            InitPattern.RANDOM_50 -> 0.50
            InitPattern.CENTER_SEED -> 0.0
            InitPattern.GRADIENT -> 0.0
            InitPattern.CHECKERBOARD -> 0.0
            InitPattern.DISK -> 0.0
            InitPattern.RING -> 0.0
            InitPattern.RANDOM_SYMMETRIC -> 0.50
            InitPattern.CROSS -> 0.0
            InitPattern.DIAGONAL -> 0.0
        }

        val useCenter = initPattern == InitPattern.CENTER_SEED
        val useGradient = initPattern == InitPattern.GRADIENT
        val isRealRule = rule is RealRule
        val useRandomReals = isRealRule && !useGradient

        // Pre-generate quadrant for RANDOM_SYMMETRIC
        val symmetricQuadrant = if (initPattern == InitPattern.RANDOM_SYMMETRIC && !isRealRule) {
            val halfW = (gridWidth + 1) / 2
            val halfH = (gridHeight + 1) / 2
            Array(halfH) { row ->
                IntArray(halfW) { col ->
                    if (Random.nextDouble() < density) {
                        if (numStates > 2) Random.nextInt(1, numStates) else 1
                    } else 0
                }
            }
        } else null

        val latticeDepth = if (selectedLatticeType.is3D) gridDepth else 1

        val lattice = createLattice(selectedLatticeType, gridWidth, gridHeight, latticeDepth) { coord ->
            when {
                useRandomReals -> Cell(RealValuedState(Random.nextDouble()), coord)
                isRealRule && useGradient -> {
                    Cell(RealValuedState(coord.col.toDouble() / gridWidth), coord)
                }
                isRealRule -> Cell(RealValuedState(0.0), coord)
                useCenter -> {
                    val atCenter = coord.row == gridHeight / 2 && coord.col == gridWidth / 2 &&
                        (!selectedLatticeType.is3D || coord.layer == latticeDepth / 2)
                    val state = if (atCenter) numStates - 1 else 0
                    Cell(IntegerCellState(state), coord)
                }
                useGradient -> {
                    val state = ((coord.col.toDouble() / gridWidth) * (numStates - 1)).toInt()
                    Cell(IntegerCellState(state), coord)
                }
                initPattern == InitPattern.CHECKERBOARD -> {
                    val state = if ((coord.row + coord.col) % 2 == 0) numStates - 1 else 0
                    Cell(IntegerCellState(state), coord)
                }
                initPattern == InitPattern.DISK -> {
                    val dx = coord.col - gridWidth / 2
                    val dy = coord.row - gridHeight / 2
                    val dz = if (selectedLatticeType.is3D) coord.layer - latticeDepth / 2 else 0
                    val r = min(gridWidth, gridHeight) / 4
                    val state = if (dx * dx + dy * dy + dz * dz <= r * r) numStates - 1 else 0
                    Cell(IntegerCellState(state), coord)
                }
                initPattern == InitPattern.RING -> {
                    val dx = coord.col - gridWidth / 2
                    val dy = coord.row - gridHeight / 2
                    val dz = if (selectedLatticeType.is3D) coord.layer - latticeDepth / 2 else 0
                    val r = min(gridWidth, gridHeight) / 4
                    val distSq = dx * dx + dy * dy + dz * dz
                    val innerR = (r * 0.6).toInt()
                    val state = if (distSq <= r * r && distSq > innerR * innerR) numStates - 1 else 0
                    Cell(IntegerCellState(state), coord)
                }
                initPattern == InitPattern.RANDOM_SYMMETRIC && symmetricQuadrant != null -> {
                    val mirrorRow = if (coord.row < gridHeight / 2) coord.row else gridHeight - 1 - coord.row
                    val mirrorCol = if (coord.col < gridWidth / 2) coord.col else gridWidth - 1 - coord.col
                    val state = symmetricQuadrant[mirrorRow.coerceIn(0, symmetricQuadrant.size - 1)][mirrorCol.coerceIn(0, symmetricQuadrant[0].size - 1)]
                    Cell(IntegerCellState(state), coord)
                }
                initPattern == InitPattern.CROSS -> {
                    val state = if (coord.row == gridHeight / 2 || coord.col == gridWidth / 2) numStates - 1 else 0
                    Cell(IntegerCellState(state), coord)
                }
                initPattern == InitPattern.DIAGONAL -> {
                    val state = if ((coord.row + coord.col) % 6 < 3) numStates - 1 else 0
                    Cell(IntegerCellState(state), coord)
                }
                else -> {
                    val state = if (Random.nextDouble() < density) {
                        if (numStates > 2) Random.nextInt(1, numStates) else 1
                    } else 0
                    Cell(IntegerCellState(state), coord)
                }
            }
        }

        engine.configure(
            SimulationConfig(
                rule = rule,
                lattice = lattice,
                colorScheme = colorSchemes[selectedColorSchemeIndex],
                updateGraphicsEveryNSteps = 1,
                numWorkers = DEFAULT_WORKERS
            )
        )
    }

    // Wire external menu bar actions
    val externalReset = AppActions.resetTrigger
    LaunchedEffect(externalReset) {
        if (externalReset > 0) {
            engine.stop()
            resetKey++
        }
    }
    val externalToggleGrid = AppActions.toggleGridTrigger
    LaunchedEffect(externalToggleGrid) {
        if (externalToggleGrid > 0) gridVisible = !gridVisible
    }
    val externalExportImage = AppActions.exportImageTrigger
    LaunchedEffect(externalExportImage) {
        if (externalExportImage > 0) {
            exportImage(cellColors, gridWidth, gridHeight)
        }
    }
    val externalStartGif = AppActions.startGifTrigger
    LaunchedEffect(externalStartGif) {
        if (externalStartGif > 0 && !gifRecording) {
            gifRecorder.startRecording(gridWidth, gridHeight)
            gifRecording = true
            gifFrameCount = 0
        }
    }
    val externalStopGif = AppActions.stopGifTrigger
    LaunchedEffect(externalStopGif) {
        if (externalStopGif > 0 && gifRecording) {
            gifRecording = false
            gifRecorder.stopAndSave()
            gifFrameCount = 0
        }
    }

    // Wire new menu bar actions
    val externalStep = AppActions.stepTrigger
    LaunchedEffect(externalStep) {
        if (externalStep > 0) engine.step()
    }
    val externalRewind = AppActions.rewindTrigger
    LaunchedEffect(externalRewind) {
        if (externalRewind > 0) engine.rewind()
    }
    val externalToggleAnalysis = AppActions.toggleAnalysisTrigger
    LaunchedEffect(externalToggleAnalysis) {
        if (externalToggleAnalysis > 0) showAnalysis = !showAnalysis
    }
    val externalFitToWindow = AppActions.fitToWindowTrigger
    LaunchedEffect(externalFitToWindow) {
        if (externalFitToWindow > 0) fitToWindowTrigger++
    }
    val externalAbout = AppActions.aboutTrigger
    LaunchedEffect(externalAbout) {
        if (externalAbout > 0) showAbout = true
    }
    val externalKeyboardHelp = AppActions.keyboardHelpTrigger
    LaunchedEffect(externalKeyboardHelp) {
        if (externalKeyboardHelp > 0) showKeyboardHelp = true
    }
    val externalHelp = AppActions.helpTrigger
    LaunchedEffect(externalHelp) {
        if (externalHelp > 0) showHelp = true
    }

    // Wire save/load triggers
    val externalSave = AppActions.saveTrigger
    LaunchedEffect(externalSave) {
        if (externalSave > 0) {
            val config = engine.config ?: return@LaunchedEffect
            val lattice = config.lattice
            val cellStates = IntArray(lattice.cellCount) { i -> lattice.getCell(i).toInt() }
            val fileData = SimulationFileData(
                ruleName = config.rule.displayName,
                latticeType = selectedLatticeType.name,
                width = gridWidth,
                height = gridHeight,
                cellStates = cellStates,
                colorSchemeIndex = selectedColorSchemeIndex,
                generation = simState.generation
            )
            withContext(Dispatchers.Default) {
                saveSimulationFile(fileData)
            }
        }
    }
    val externalLoad = AppActions.loadTrigger
    LaunchedEffect(externalLoad) {
        if (externalLoad > 0) {
            val fileData = withContext(Dispatchers.Default) { loadSimulationFile() }
            if (fileData != null) {
                // Find matching rule by name
                val ruleIdx = rules.indexOfFirst { it.displayName == fileData.ruleName }
                if (ruleIdx >= 0) {
                    val latticeType = try { LatticeType.valueOf(fileData.latticeType) } catch (_: Exception) { null }
                    if (latticeType != null) {
                        selectedLatticeType = latticeType
                    }
                    gridWidth = fileData.width
                    gridHeight = fileData.height
                    selectedColorSchemeIndex = fileData.colorSchemeIndex.coerceIn(0, colorSchemes.lastIndex)
                    selectedRuleIndex = ruleIdx

                    // Wait for the LaunchedEffect above to reinitialize, then overwrite cell states
                    kotlinx.coroutines.delay(100)
                    val config = engine.config
                    if (config != null) {
                        val lattice = config.lattice
                        for (i in fileData.cellStates.indices) {
                            if (i < lattice.cellCount) {
                                lattice.getCell(i).resetState(IntegerCellState(fileData.cellStates[i]))
                            }
                        }
                        // Refresh the color buffer
                        engine.updateColorScheme(colorSchemes[selectedColorSchemeIndex])
                    }
                }
            }
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose { engine.destroy() }
    }

    // Focus for keyboard shortcuts
    val focusRequester = remember { FocusRequester() }

    // Helper for play/pause toggle
    fun togglePlayPause() {
        when (simState.status) {
            SimulationStatus.RUNNING -> engine.pause()
            SimulationStatus.PAUSED -> engine.resume()
            SimulationStatus.IDLE -> engine.start()
            SimulationStatus.STEPPING -> {}
        }
    }

    // Helper for toggling draw mode
    fun toggleDrawMode() {
        if (!drawMode) {
            // Entering draw mode — auto-pause if running
            if (simState.status == SimulationStatus.RUNNING) {
                statusBeforeDrawMode = SimulationStatus.RUNNING
                engine.pause()
            } else {
                statusBeforeDrawMode = null
            }
            drawMode = true
        } else {
            // Exiting draw mode — resume if was running
            drawMode = false
            if (statusBeforeDrawMode == SimulationStatus.RUNNING) {
                engine.resume()
            }
            statusBeforeDrawMode = null
        }
    }

    // Wire menu bar actions that depend on local functions
    val externalTogglePlayPause = AppActions.togglePlayPauseTrigger
    LaunchedEffect(externalTogglePlayPause) {
        if (externalTogglePlayPause > 0) togglePlayPause()
    }
    val externalToggleDrawMode = AppActions.toggleDrawModeTrigger
    LaunchedEffect(externalToggleDrawMode) {
        if (externalToggleDrawMode > 0) toggleDrawMode()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    val isModifier = event.isCtrlPressed || event.isMetaPressed
                    when {
                        isModifier && event.key == Key.S -> {
                            AppActions.requestSave(); true
                        }
                        isModifier && event.key == Key.O -> {
                            AppActions.requestLoad(); true
                        }
                        else -> when (event.key) {
                        Key.Spacebar -> { togglePlayPause(); true }
                        Key.S -> { engine.step(); true }
                        Key.R -> { engine.rewind(); true }
                        Key.G -> { gridVisible = !gridVisible; true }
                        Key.F -> { fitToWindowTrigger++; true }
                        Key.D -> { toggleDrawMode(); true }
                        Key.A -> { showAnalysis = !showAnalysis; true }
                        Key.E -> {
                            exportImage(cellColors, gridWidth, gridHeight)
                            true
                        }
                        Key.Equals -> {
                            if (speedIndex > 0) speedIndex--
                            true
                        }
                        Key.Minus -> {
                            if (speedIndex < speedSteps.lastIndex) speedIndex++
                            true
                        }
                        Key.Slash -> {
                            if (event.isShiftPressed) { showKeyboardHelp = true; true }
                            else false
                        }
                        Key.H -> { showHelp = !showHelp; true }
                        Key.F1 -> { showKeyboardHelp = true; true }
                        else -> false
                    }
                    }
                } else false
            }
    ) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("CA Explorer")
                    },
                    actions = {
                        // Status badge with animated color and proper icons
                        val statusText = when (simState.status) {
                            SimulationStatus.RUNNING -> "Running"
                            SimulationStatus.PAUSED -> "Paused"
                            SimulationStatus.IDLE -> "Idle"
                            SimulationStatus.STEPPING -> "Step"
                        }
                        val statusIcon = when (simState.status) {
                            SimulationStatus.RUNNING -> Icons.Default.Pause
                            SimulationStatus.PAUSED -> Icons.Default.PlayArrow
                            SimulationStatus.IDLE -> Icons.Default.PlayArrow
                            SimulationStatus.STEPPING -> Icons.Default.SkipNext
                        }
                        val targetStatusColor = when (simState.status) {
                            SimulationStatus.RUNNING -> MaterialTheme.colorScheme.primary
                            SimulationStatus.PAUSED -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.outline
                        }
                        val statusColor by animateColorAsState(
                            targetValue = targetStatusColor,
                            animationSpec = tween(300),
                            label = "status-color"
                        )
                        AssistChip(
                            onClick = { togglePlayPause() },
                            label = { Text(statusText) },
                            leadingIcon = {
                                Crossfade(
                                    targetState = statusIcon,
                                    label = "status-icon"
                                ) { icon ->
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = statusColor
                                    )
                                }
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = statusColor.copy(alpha = 0.12f),
                                labelColor = statusColor
                            ),
                            modifier = Modifier.padding(end = 4.dp)
                        )

                        // Analysis panel toggle
                        IconButton(onClick = { showAnalysis = !showAnalysis }) {
                            Icon(
                                Icons.Default.BarChart,
                                contentDescription = "Analysis (A)",
                                tint = if (showAnalysis) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Draw mode toggle
                        IconButton(onClick = { toggleDrawMode() }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Draw mode (D)",
                                tint = if (drawMode) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Grid toggle
                        IconButton(onClick = { gridVisible = !gridVisible }) {
                            Icon(
                                Icons.Default.GridOn,
                                contentDescription = "Toggle grid (G)",
                                tint = if (gridVisible) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Settings toggle
                        IconButton(onClick = { showConfig = !showConfig }) {
                            Icon(Icons.Default.Tune, contentDescription = "Settings")
                        }

                        // GIF record toggle
                        IconButton(onClick = {
                            if (gifRecording) {
                                gifRecording = false
                                gifRecorder.stopAndSave()
                                gifFrameCount = 0
                            } else {
                                gifRecorder.startRecording(gridWidth, gridHeight)
                                gifRecording = true
                                gifFrameCount = 0
                            }
                        }) {
                            if (gifRecording) {
                                BadgedBox(
                                    badge = {
                                        Badge { Text("$gifFrameCount") }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Stop,
                                        contentDescription = "Stop recording",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            } else {
                                Icon(
                                    Icons.Default.FiberManualRecord,
                                    contentDescription = "Record GIF",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Export image
                        IconButton(onClick = {
                            exportImage(cellColors, gridWidth, gridHeight)
                        }) {
                            Icon(
                                Icons.Default.SaveAlt,
                                contentDescription = "Export image (E)"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                StatusBar(
                    simState = simState,
                    gridWidth = gridWidth,
                    gridHeight = gridHeight,
                    ruleName = rules.getOrNull(selectedRuleIndex)?.displayName ?: "None",
                    drawMode = drawMode
                )
            },
            floatingActionButton = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Step
                    SmallFloatingActionButton(
                        onClick = { engine.step() },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Icon(Icons.Default.SkipNext, "Step (S)")
                    }

                    // Rewind
                    SmallFloatingActionButton(
                        onClick = { engine.rewind() },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Icon(Icons.Default.Replay, "Rewind (R)")
                    }

                    // Reset (stop + reinitialize)
                    SmallFloatingActionButton(
                        onClick = {
                            engine.stop()
                            resetKey++
                        },
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Icon(Icons.Default.RestartAlt, "Reset")
                    }

                    // Play/Pause with animated icon transition
                    FloatingActionButton(
                        onClick = { togglePlayPause() },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Crossfade(
                            targetState = simState.status == SimulationStatus.RUNNING,
                            label = "play-pause"
                        ) { isRunning ->
                            Icon(
                                if (isRunning) Icons.Default.Pause
                                else Icons.Default.PlayArrow,
                                contentDescription = if (isRunning) "Pause" else "Play"
                            )
                        }
                    }
                }
            }
        ) { padding ->
            var showRulePicker by remember { mutableStateOf(false) }
            val currentRuleName = rules.getOrNull(selectedRuleIndex)?.displayName ?: "None"

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Analysis panel (left side) with animated visibility
                AnimatedVisibility(
                    visible = showAnalysis,
                    enter = slideInHorizontally(initialOffsetX = { -it }),
                    exit = slideOutHorizontally(targetOffsetX = { -it })
                ) {
                    Row {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxHeight().width(300.dp)
                        ) {
                            AnalysisDashboard(
                                results = analysisResults,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        VerticalDivider()
                    }
                }

                // Left: Canvas + Status Bar
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // CA rendering canvas
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        if (selectedLatticeType.is3D) {
                            val numStates = (currentRule as? IntegerRule)?.numStates
                                ?: (rules.getOrNull(selectedRuleIndex) as? IntegerRule)?.numStates
                                ?: 2
                            VoxelCanvas(
                                cellColors = cellColors,
                                cellStates = cellStates,
                                gridWidth = gridWidth,
                                gridHeight = gridHeight,
                                gridDepth = gridDepth,
                                numStates = numStates,
                                voxelOpacity = voxelOpacity,
                                depthFadeEnabled = depthFadeEnabled,
                                depthFadeReversed = depthFadeReversed,
                                layerMin = layerMin,
                                layerMax = layerMax,
                                modifier = Modifier.fillMaxSize().alpha(canvasAlpha)
                            )
                        } else {
                            SimulationCanvas(
                                cellColors = cellColors,
                                gridWidth = gridWidth,
                                gridHeight = gridHeight,
                                gridVisible = gridVisible,
                                fitToWindowTrigger = fitToWindowTrigger,
                                drawMode = drawMode,
                                smoothInterpolation = smoothEnabled,
                                onCellToggle = { col, row -> engine.toggleCell(row, col) },
                                onCellPaint = { col, row ->
                                    val numStates = (rules.getOrNull(selectedRuleIndex) as? IntegerRule)?.numStates ?: 2
                                    engine.paintCell(row, col, numStates - 1)
                                },
                                onPaintFinished = { engine.flushPaint() },
                                modifier = Modifier.fillMaxSize().alpha(canvasAlpha)
                            )
                        }

                        // Rule selector chip
                        ElevatedFilterChip(
                            selected = true,
                            onClick = { showRulePicker = true },
                            label = {
                                Text(
                                    currentRuleName,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
                        )
                    }
                }

                // Side panel with animated visibility
                AnimatedVisibility(
                    visible = showConfig,
                    enter = slideInHorizontally(initialOffsetX = { it }),
                    exit = slideOutHorizontally(targetOffsetX = { it })
                ) {
                    Row {
                        VerticalDivider()
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            ConfigPanel(
                                gridWidth = gridWidth,
                                gridHeight = gridHeight,
                                onGridSizeChanged = { w, h -> gridWidth = w; gridHeight = h },
                                gridDepth = gridDepth,
                                onGridDepthChanged = { gridDepth = it },
                                colorSchemes = colorSchemes,
                                selectedColorSchemeIndex = selectedColorSchemeIndex,
                                onColorSchemeChanged = { selectedColorSchemeIndex = it },
                                simulationDelay = simulationDelay,
                                speedSteps = speedSteps,
                                speedIndex = speedIndex,
                                onSpeedIndexChanged = { speedIndex = it },
                                initPattern = initPattern,
                                onInitPatternChanged = { initPattern = it },
                                selectedLatticeType = selectedLatticeType,
                                onLatticeTypeChanged = { selectedLatticeType = it },
                                selectedPalette = selectedPalette,
                                onPaletteChanged = onPaletteChanged,
                                currentRule = currentRule,
                                onRuleChanged = { newRule ->
                                    currentRule = newRule
                                    resetKey++
                                },
                                onResetSimulation = { engine.stop(); resetKey++ },
                                onDismiss = { showConfig = false },
                                trailEnabled = trailEnabled,
                                onTrailEnabledChanged = { trailEnabled = it },
                                trailDecay = trailDecay,
                                onTrailDecayChanged = { trailDecay = it },
                                bloomEnabled = bloomEnabled,
                                onBloomEnabledChanged = { bloomEnabled = it },
                                bloomIntensity = bloomIntensity,
                                onBloomIntensityChanged = { bloomIntensity = it },
                                smoothEnabled = smoothEnabled,
                                onSmoothEnabledChanged = { smoothEnabled = it },
                                voxelOpacity = voxelOpacity,
                                onVoxelOpacityChanged = { voxelOpacity = it },
                                depthFadeEnabled = depthFadeEnabled,
                                onDepthFadeEnabledChanged = { depthFadeEnabled = it },
                                depthFadeReversed = depthFadeReversed,
                                onDepthFadeReversedChanged = { depthFadeReversed = it },
                                layerMin = layerMin,
                                onLayerMinChanged = { layerMin = it },
                                layerMax = layerMax,
                                onLayerMaxChanged = { layerMax = it }
                            )
                        }
                    }
                }
            }

            // Rule picker bottom sheet
            if (showRulePicker) {
                RulePickerSheet(
                    rules = rules,
                    selectedRule = rules.getOrNull(selectedRuleIndex),
                    onRuleSelected = { rule ->
                        selectedRuleIndex = rules.indexOf(rule)
                    },
                    onDismiss = { showRulePicker = false }
                )
            }

            // About dialog
            if (showAbout) {
                AboutDialog(onDismiss = { showAbout = false })
            }

            // Keyboard shortcuts help dialog
            if (showKeyboardHelp) {
                KeyboardShortcutsSheet(onDismiss = { showKeyboardHelp = false })
            }

            // User Guide help dialog
            if (showHelp) {
                HelpDialog(onDismiss = { showHelp = false })
            }
        }
    }
}

/**
 * Bottom status bar showing simulation info.
 */
@Composable
fun StatusBar(
    simState: SimulationState,
    gridWidth: Int,
    gridHeight: Int,
    ruleName: String,
    drawMode: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: rule name + grid size
            Text(
                "$ruleName • ${gridWidth}×${gridHeight}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Center: generation + speed
            Text(
                "Gen ${simState.generation} • ${simState.generationsPerSecond.toInt()} gen/s",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Right: draw mode indicator + cell count
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (drawMode) {
                    Text(
                        "✏️ Draw",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    "${gridWidth * gridHeight} cells",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
