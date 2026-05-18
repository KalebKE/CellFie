package org.caexplorer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.caexplorer.domain.cell.Cell
import org.caexplorer.domain.cellstate.IntegerCellState
import org.caexplorer.domain.colorscheme.RainbowColorScheme
import org.caexplorer.domain.lattice.SquareLattice
import org.caexplorer.domain.rule.IntegerRule
import org.caexplorer.domain.rule.RuleRegistry
import org.caexplorer.domain.util.Coordinate
import org.caexplorer.engine.*
import org.caexplorer.ui.components.SimulationCanvas

/**
 * Main simulation screen with the CA canvas and Material 3 controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    // Engine state
    val engine = remember { SimulationEngine() }
    val simState by engine.state.collectAsState()
    val cellColors by engine.cellColors.collectAsState()

    // Grid configuration
    var gridWidth by remember { mutableStateOf(200) }
    var gridHeight by remember { mutableStateOf(200) }
    var gridVisible by remember { mutableStateOf(false) }

    // Rule selection
    val rules = remember { RuleRegistry.getFeaturedRules() }
    var selectedRuleIndex by remember { mutableStateOf(0) }

    // Initialize simulation on first composition or when rule changes
    LaunchedEffect(selectedRuleIndex, gridWidth, gridHeight) {
        val rule = rules.getOrNull(selectedRuleIndex) ?: return@LaunchedEffect
        val numStates = (rule as? IntegerRule)?.numStates ?: 2

        val lattice = SquareLattice(gridWidth, gridHeight) { coord ->
            Cell(IntegerCellState((0 until numStates).random()), coord)
        }

        engine.configure(
            SimulationConfig(
                rule = rule,
                lattice = lattice,
                colorScheme = RainbowColorScheme(),
                updateGraphicsEveryNSteps = 1,
                numWorkers = DEFAULT_WORKERS
            )
        )
    }

    // Clean up engine on dispose
    DisposableEffect(Unit) {
        onDispose { engine.destroy() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CA Explorer") },
                actions = {
                    // Generation counter
                    AssistChip(
                        onClick = {},
                        label = { Text("Gen ${simState.generation}") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    // Speed indicator
                    if (simState.status == SimulationStatus.RUNNING) {
                        AssistChip(
                            onClick = {},
                            label = { Text("${simState.generationsPerSecond.toInt()} gen/s") },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    // Grid toggle
                    IconButton(onClick = { gridVisible = !gridVisible }) {
                        Text(
                            "⊞",
                            color = if (gridVisible) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
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
                // Step button
                SmallFloatingActionButton(
                    onClick = { engine.step() },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text("⏭")
                }

                // Rewind button
                SmallFloatingActionButton(
                    onClick = { engine.rewind() },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text("⏮")
                }

                // Play/Pause FAB
                FloatingActionButton(
                    onClick = {
                        when (simState.status) {
                            SimulationStatus.RUNNING -> engine.pause()
                            SimulationStatus.PAUSED -> engine.resume()
                            SimulationStatus.IDLE -> engine.start()
                            SimulationStatus.STEPPING -> {}
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(
                        if (simState.status == SimulationStatus.RUNNING) "⏸" else "▶",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            // CA rendering canvas
            SimulationCanvas(
                cellColors = cellColors,
                gridWidth = gridWidth,
                gridHeight = gridHeight,
                gridVisible = gridVisible,
                modifier = Modifier.fillMaxSize()
            )

            // Rule selector chip
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
            ) {
                ElevatedFilterChip(
                    selected = true,
                    onClick = {
                        selectedRuleIndex = (selectedRuleIndex + 1) % rules.size
                    },
                    label = {
                        Text(
                            rules.getOrNull(selectedRuleIndex)?.displayName ?: "None",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
            }
        }
    }
}
