package org.caexplorer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.caexplorer.domain.colorscheme.ColorScheme as CAColorScheme
import org.caexplorer.domain.lattice.LatticeType
import org.caexplorer.domain.rule.Rule
import org.caexplorer.ui.screens.InitPattern
import kotlin.math.roundToInt

/**
 * Collapsible section header used within the config panel.
 */
@Composable
private fun SectionHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        IconButton(onClick = onToggle, modifier = Modifier.size(24.dp)) {
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Configuration panel for simulation settings including grid size,
 * color scheme, speed control, and initialization pattern.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigPanel(
    gridWidth: Int,
    gridHeight: Int,
    onGridSizeChanged: (width: Int, height: Int) -> Unit,
    colorSchemes: List<CAColorScheme>,
    selectedColorSchemeIndex: Int,
    onColorSchemeChanged: (Int) -> Unit,
    simulationDelay: Long,
    speedSteps: List<Long>,
    speedIndex: Int,
    onSpeedIndexChanged: (Int) -> Unit,
    initPattern: InitPattern,
    onInitPatternChanged: (InitPattern) -> Unit,
    selectedLatticeType: LatticeType,
    onLatticeTypeChanged: (LatticeType) -> Unit,
    currentRule: Rule? = null,
    onRuleChanged: ((Rule) -> Unit)? = null,
    onResetSimulation: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var widthSlider by remember(gridWidth) { mutableStateOf(gridWidth.toFloat()) }
    var heightSlider by remember(gridHeight) { mutableStateOf(gridHeight.toFloat()) }

    var latticeTypeExpanded by remember { mutableStateOf(true) }
    var colorSchemeExpanded by remember { mutableStateOf(true) }
    var speedExpanded by remember { mutableStateOf(true) }
    var initPatternExpanded by remember { mutableStateOf(true) }
    var gridSizeExpanded by remember { mutableStateOf(true) }
    var rulePropsExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .width(300.dp)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Configuration",
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, "Close")
            }
        }

        HorizontalDivider()

        // --- Color Scheme ---
        SectionHeader("Color Scheme", colorSchemeExpanded) { colorSchemeExpanded = !colorSchemeExpanded }
        AnimatedVisibility(
            visible = colorSchemeExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Box {
                var csExpanded by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = { csExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        colorSchemes.getOrNull(selectedColorSchemeIndex)?.displayName ?: "Rainbow",
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ArrowDropDown, null)
                }
                DropdownMenu(
                    expanded = csExpanded,
                    onDismissRequest = { csExpanded = false }
                ) {
                    colorSchemes.forEachIndexed { index, scheme ->
                        DropdownMenuItem(
                            text = { Text(scheme.displayName) },
                            onClick = {
                                onColorSchemeChanged(index)
                                csExpanded = false
                            }
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        // --- Lattice Type ---
        SectionHeader("Lattice Type", latticeTypeExpanded) { latticeTypeExpanded = !latticeTypeExpanded }
        AnimatedVisibility(
            visible = latticeTypeExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box {
                    var ltExpanded by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = { ltExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedLatticeType.displayName, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(
                        expanded = ltExpanded,
                        onDismissRequest = { ltExpanded = false }
                    ) {
                        LatticeType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(type.displayName)
                                        Text(
                                            type.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    onLatticeTypeChanged(type)
                                    ltExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        // --- Speed Control ---
        SectionHeader("Speed", speedExpanded) { speedExpanded = !speedExpanded }
        AnimatedVisibility(
            visible = speedExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (simulationDelay == 0L) "Max Speed" else "Delay: ${simulationDelay}ms",
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(
                    value = speedIndex.toFloat(),
                    onValueChange = { onSpeedIndexChanged(it.roundToInt()) },
                    valueRange = 0f..(speedSteps.size - 1).toFloat(),
                    steps = speedSteps.size - 2
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("⚡ Fast", style = MaterialTheme.typography.labelSmall)
                    Text("🐌 Slow", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        HorizontalDivider()

        // --- Initialization Pattern ---
        SectionHeader("Init Pattern", initPatternExpanded) { initPatternExpanded = !initPatternExpanded }
        AnimatedVisibility(
            visible = initPatternExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Box {
                var ipExpanded by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = { ipExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(initPattern.displayName, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, null)
                }
                DropdownMenu(
                    expanded = ipExpanded,
                    onDismissRequest = { ipExpanded = false }
                ) {
                    InitPattern.entries.forEach { pattern ->
                        DropdownMenuItem(
                            text = { Text(pattern.displayName) },
                            onClick = {
                                onInitPatternChanged(pattern)
                                ipExpanded = false
                            }
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        // --- Grid Size ---
        SectionHeader("Grid Size", gridSizeExpanded) { gridSizeExpanded = !gridSizeExpanded }
        AnimatedVisibility(
            visible = gridSizeExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Width
                Text(
                    "Width: ${widthSlider.roundToInt()}",
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(
                    value = widthSlider,
                    onValueChange = { widthSlider = it },
                    onValueChangeFinished = {
                        onGridSizeChanged(widthSlider.roundToInt(), heightSlider.roundToInt())
                    },
                    valueRange = 50f..1000f,
                    steps = 18
                )

                // Height
                Text(
                    "Height: ${heightSlider.roundToInt()}",
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(
                    value = heightSlider,
                    onValueChange = { heightSlider = it },
                    onValueChangeFinished = {
                        onGridSizeChanged(widthSlider.roundToInt(), heightSlider.roundToInt())
                    },
                    valueRange = 50f..1000f,
                    steps = 18
                )

                // Presets
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuggestionChip(
                        onClick = {
                            widthSlider = 100f; heightSlider = 100f
                            onGridSizeChanged(100, 100)
                        },
                        label = { Text("100²") }
                    )
                    SuggestionChip(
                        onClick = {
                            widthSlider = 200f; heightSlider = 200f
                            onGridSizeChanged(200, 200)
                        },
                        label = { Text("200²") }
                    )
                    SuggestionChip(
                        onClick = {
                            widthSlider = 500f; heightSlider = 500f
                            onGridSizeChanged(500, 500)
                        },
                        label = { Text("500²") }
                    )
                }
            }
        }

        HorizontalDivider()

        // --- Rule Properties ---
        if (currentRule != null && onRuleChanged != null) {
            SectionHeader("Rule Properties", rulePropsExpanded) { rulePropsExpanded = !rulePropsExpanded }
            AnimatedVisibility(
                visible = rulePropsExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                RulePropertiesPanel(
                    rule = currentRule,
                    onRuleChanged = onRuleChanged
                )
            }

            HorizontalDivider()
        }

        // --- Reset Button ---
        Button(
            onClick = onResetSimulation,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                Icons.Default.RestartAlt,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Reset Simulation")
        }
    }
}
