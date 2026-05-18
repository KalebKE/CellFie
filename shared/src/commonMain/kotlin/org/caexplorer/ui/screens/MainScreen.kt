package org.caexplorer.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.isSystemInDarkTheme
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
import org.caexplorer.domain.cell.Cell
import org.caexplorer.domain.cellstate.IntegerCellState
import org.caexplorer.domain.cellstate.RealValuedState
import org.caexplorer.domain.colorscheme.ALL_COLOR_SCHEMES
import org.caexplorer.domain.lattice.SquareLattice
import org.caexplorer.domain.rule.*
import org.caexplorer.engine.*
import org.caexplorer.ui.AppActions
import org.caexplorer.ui.components.AnalysisDashboard
import org.caexplorer.ui.components.ConfigPanel
import org.caexplorer.ui.components.RulePickerSheet
import org.caexplorer.ui.components.SimulationCanvas
import org.caexplorer.ui.theme.ThemeState
import org.caexplorer.ui.util.exportImage
import kotlin.random.Random

/**
 * Initialization patterns for the simulation grid.
 */
enum class InitPattern(val displayName: String) {
    AUTO("Auto (rule-based)"),
    RANDOM_25("Random 25%"),
    RANDOM_50("Random 50%"),
    CENTER_SEED("Center Seed"),
    GRADIENT("Gradient")
}

/**
 * Main simulation screen with the CA canvas, Material 3 controls,
 * keyboard shortcuts, speed control, and theme toggling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    // Engine
    val engine = remember { SimulationEngine() }
    val simState by engine.state.collectAsState()
    val cellColors by engine.cellColors.collectAsState()

    // Grid configuration — load from settings
    var gridWidth by remember { mutableStateOf(AppSettings.getInt(SettingsKeys.GRID_WIDTH, 200)) }
    var gridHeight by remember { mutableStateOf(AppSettings.getInt(SettingsKeys.GRID_HEIGHT, 200)) }
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

    // Init pattern
    var initPattern by remember { mutableStateOf(InitPattern.AUTO) }

    // Reset key — incrementing triggers re-initialization
    var resetKey by remember { mutableStateOf(0) }

    // Fit-to-window trigger for canvas zoom reset
    var fitToWindowTrigger by remember { mutableStateOf(0) }

    // Draw mode
    var drawMode by remember { mutableStateOf(false) }
    // Track status before entering draw mode so we can restore it
    var statusBeforeDrawMode by remember { mutableStateOf<SimulationStatus?>(null) }

    // Canvas fade animation for rule switches
    var canvasFadeTrigger by remember { mutableStateOf(0) }
    val canvasAlpha by animateFloatAsState(
        targetValue = if (canvasFadeTrigger % 2 == 0) 1f else 0.3f,
        animationSpec = tween(durationMillis = 150),
        label = "canvas-fade"
    )

    // Load dark theme from settings on startup
    LaunchedEffect(Unit) {
        val savedDark = AppSettings.getBoolean(SettingsKeys.DARK_THEME, false)
        if (ThemeState.useDarkTheme == null) {
            ThemeState.useDarkTheme = savedDark
        }
    }

    // Theme
    val isDark = ThemeState.useDarkTheme ?: isSystemInDarkTheme()

    // Propagate speed changes to engine
    LaunchedEffect(simulationDelay) {
        engine.setSpeed(simulationDelay)
    }

    // Propagate color scheme changes (without reinitializing simulation)
    LaunchedEffect(selectedColorSchemeIndex) {
        engine.updateColorScheme(colorSchemes[selectedColorSchemeIndex])
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

    // Persist settings on changes
    LaunchedEffect(gridWidth, gridHeight) {
        AppSettings.putInt(SettingsKeys.GRID_WIDTH, gridWidth)
        AppSettings.putInt(SettingsKeys.GRID_HEIGHT, gridHeight)
    }
    LaunchedEffect(selectedRuleIndex) {
        AppSettings.putInt(SettingsKeys.LAST_RULE_INDEX, selectedRuleIndex)
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
    LaunchedEffect(isDark) {
        AppSettings.putBoolean(SettingsKeys.DARK_THEME, isDark)
    }

    // Initialize simulation on rule/grid/reset changes
    LaunchedEffect(selectedRuleIndex, gridWidth, gridHeight, resetKey) {
        val rule = rules.getOrNull(selectedRuleIndex) ?: return@LaunchedEffect
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
        }

        val useCenter = initPattern == InitPattern.CENTER_SEED
        val useGradient = initPattern == InitPattern.GRADIENT
        val isRealRule = rule is RealRule
        val useRandomReals = isRealRule && !useGradient

        val lattice = SquareLattice(gridWidth, gridHeight) { coord ->
            when {
                useRandomReals -> Cell(RealValuedState(Random.nextDouble()), coord)
                isRealRule && useGradient -> {
                    Cell(RealValuedState(coord.col.toDouble() / gridWidth), coord)
                }
                isRealRule -> Cell(RealValuedState(0.0), coord)
                useCenter -> {
                    val state = if (coord.row == gridHeight / 2 && coord.col == gridWidth / 2)
                        numStates - 1 else 0
                    Cell(IntegerCellState(state), coord)
                }
                useGradient -> {
                    val state = ((coord.col.toDouble() / gridWidth) * (numStates - 1)).toInt()
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
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
                        else -> false
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
                        // Status badge with animated color
                        val statusText = when (simState.status) {
                            SimulationStatus.RUNNING -> "▶ Running"
                            SimulationStatus.PAUSED -> "⏸ Paused"
                            SimulationStatus.IDLE -> "⏹ Idle"
                            SimulationStatus.STEPPING -> "⏭ Step"
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
                            onClick = {},
                            label = { Text(statusText) },
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

                        // Theme toggle
                        IconButton(onClick = {
                            ThemeState.useDarkTheme = !isDark
                        }) {
                            Icon(
                                if (isDark) Icons.Default.LightMode
                                else Icons.Default.DarkMode,
                                contentDescription = "Toggle theme"
                            )
                        }

                        // Settings toggle
                        IconButton(onClick = { showConfig = !showConfig }) {
                            Icon(Icons.Default.Tune, contentDescription = "Settings")
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
                        SimulationCanvas(
                            cellColors = cellColors,
                            gridWidth = gridWidth,
                            gridHeight = gridHeight,
                            gridVisible = gridVisible,
                            fitToWindowTrigger = fitToWindowTrigger,
                            drawMode = drawMode,
                            onCellToggle = { col, row -> engine.toggleCell(row, col) },
                            onCellPaint = { col, row ->
                                val numStates = (rules.getOrNull(selectedRuleIndex) as? IntegerRule)?.numStates ?: 2
                                engine.paintCell(row, col, numStates - 1)
                            },
                            onPaintFinished = { engine.flushPaint() },
                            modifier = Modifier.fillMaxSize().alpha(canvasAlpha)
                        )

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

                    // Bottom Status Bar
                    StatusBar(
                        simState = simState,
                        gridWidth = gridWidth,
                        gridHeight = gridHeight,
                        ruleName = currentRuleName,
                        drawMode = drawMode
                    )
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
                                colorSchemes = colorSchemes,
                                selectedColorSchemeIndex = selectedColorSchemeIndex,
                                onColorSchemeChanged = { selectedColorSchemeIndex = it },
                                simulationDelay = simulationDelay,
                                speedSteps = speedSteps,
                                speedIndex = speedIndex,
                                onSpeedIndexChanged = { speedIndex = it },
                                initPattern = initPattern,
                                onInitPatternChanged = { initPattern = it },
                                onResetSimulation = { engine.stop(); resetKey++ },
                                onDismiss = { showConfig = false }
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
