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
import org.caexplorer.ui.theme.AppPalette
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
    gridDepth: Int = 20,
    onGridDepthChanged: (Int) -> Unit = {},
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
    selectedPalette: AppPalette = AppPalette.FIRE,
    onPaletteChanged: (AppPalette) -> Unit = {},
    currentRule: Rule? = null,
    onRuleChanged: ((Rule) -> Unit)? = null,
    onOpenRulePicker: () -> Unit = {},
    onResetSimulation: () -> Unit,
    onDismiss: () -> Unit,
    trailEnabled: Boolean = false,
    onTrailEnabledChanged: (Boolean) -> Unit = {},
    trailDecay: Float = 0.92f,
    onTrailDecayChanged: (Float) -> Unit = {},
    bloomEnabled: Boolean = false,
    onBloomEnabledChanged: (Boolean) -> Unit = {},
    bloomIntensity: Float = 0.6f,
    onBloomIntensityChanged: (Float) -> Unit = {},
    smoothEnabled: Boolean = false,
    onSmoothEnabledChanged: (Boolean) -> Unit = {},
    voxelOpacity: Float = 0.8f,
    onVoxelOpacityChanged: (Float) -> Unit = {},
    depthFadeEnabled: Boolean = false,
    onDepthFadeEnabledChanged: (Boolean) -> Unit = {},
    depthFadeReversed: Boolean = false,
    onDepthFadeReversedChanged: (Boolean) -> Unit = {},
    layerMin: Int = 0,
    onLayerMinChanged: (Int) -> Unit = {},
    layerMax: Int = Int.MAX_VALUE,
    onLayerMaxChanged: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var widthSlider by remember(gridWidth) { mutableStateOf(gridWidth.toFloat()) }
    var heightSlider by remember(gridHeight) { mutableStateOf(gridHeight.toFloat()) }
    var depthSlider by remember(gridDepth) { mutableStateOf(gridDepth.toFloat()) }

    var latticeTypeExpanded by remember { mutableStateOf(true) }
    var colorSchemeExpanded by remember { mutableStateOf(true) }
    var appThemeExpanded by remember { mutableStateOf(true) }
    var speedExpanded by remember { mutableStateOf(true) }
    var initPatternExpanded by remember { mutableStateOf(true) }
    var gridSizeExpanded by remember { mutableStateOf(true) }
    var renderFxExpanded by remember { mutableStateOf(true) }
    var voxelOpacityExpanded by remember { mutableStateOf(true) }

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

        // --- Current Rule (always visible, first section) ---
        if (currentRule != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Rule",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Rule name + category
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        currentRule.displayName,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(4.dp))
                    SuggestionChip(
                        onClick = {},
                        label = { Text(currentRule.category.displayName) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.height(24.dp)
                    )
                }
            }

            // Change Rule button
            Button(
                onClick = onOpenRulePicker,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.SwapHoriz,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Change Rule")
            }

            HorizontalDivider()

            // Rule properties (always visible)
            if (onRuleChanged != null) {
                RulePropertiesPanel(
                    rule = currentRule,
                    onRuleChanged = onRuleChanged
                )
                HorizontalDivider()
            }
        }

        // --- App Theme ---
        SectionHeader("App Theme", appThemeExpanded) { appThemeExpanded = !appThemeExpanded }
        AnimatedVisibility(
            visible = appThemeExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Box {
                var atExpanded by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = { atExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(selectedPalette.displayName, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, null)
                }
                DropdownMenu(
                    expanded = atExpanded,
                    onDismissRequest = { atExpanded = false }
                ) {
                    AppPalette.entries.forEach { palette ->
                        DropdownMenuItem(
                            text = { Text(palette.displayName) },
                            onClick = {
                                onPaletteChanged(palette)
                                atExpanded = false
                            }
                        )
                    }
                }
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

        // --- Render FX ---
        SectionHeader("Render FX", renderFxExpanded) { renderFxExpanded = !renderFxExpanded }
        AnimatedVisibility(
            visible = renderFxExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Trail/Fade toggle + decay slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Trail / Fade", style = MaterialTheme.typography.bodySmall)
                    Switch(
                        checked = trailEnabled,
                        onCheckedChange = onTrailEnabledChanged,
                        modifier = Modifier.height(24.dp)
                    )
                }
                if (trailEnabled) {
                    Text(
                        "Trail length: ${((1f - trailDecay) * 100).roundToInt()}% decay",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = trailDecay,
                        onValueChange = onTrailDecayChanged,
                        valueRange = 0.80f..0.99f
                    )
                }

                // Bloom/Glow toggle + intensity slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Bloom / Glow", style = MaterialTheme.typography.bodySmall)
                    Switch(
                        checked = bloomEnabled,
                        onCheckedChange = onBloomEnabledChanged,
                        modifier = Modifier.height(24.dp)
                    )
                }
                if (bloomEnabled) {
                    Text(
                        "Intensity: ${(bloomIntensity * 100).roundToInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = bloomIntensity,
                        onValueChange = onBloomIntensityChanged,
                        valueRange = 0.1f..1.5f
                    )
                }

                // Smooth interpolation toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Smooth / Painterly", style = MaterialTheme.typography.bodySmall)
                    Switch(
                        checked = smoothEnabled,
                        onCheckedChange = onSmoothEnabledChanged,
                        modifier = Modifier.height(24.dp)
                    )
                }

                // Preset combos
                Text(
                    "Presets",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SuggestionChip(
                        onClick = {
                            onTrailEnabledChanged(true)
                            onTrailDecayChanged(0.94f)
                            onBloomEnabledChanged(true)
                            onBloomIntensityChanged(0.8f)
                            onSmoothEnabledChanged(true)
                        },
                        label = { Text("Ethereal") }
                    )
                    SuggestionChip(
                        onClick = {
                            onTrailEnabledChanged(true)
                            onTrailDecayChanged(0.88f)
                            onBloomEnabledChanged(true)
                            onBloomIntensityChanged(1.2f)
                            onSmoothEnabledChanged(false)
                        },
                        label = { Text("Neon") }
                    )
                    SuggestionChip(
                        onClick = {
                            onTrailEnabledChanged(false)
                            onBloomEnabledChanged(false)
                            onSmoothEnabledChanged(false)
                        },
                        label = { Text("Classic") }
                    )
                }
            }
        }

        HorizontalDivider()

        // --- 3D Opacity (only for 3D lattices) ---
        if (selectedLatticeType.is3D) {
            SectionHeader("3D Opacity", voxelOpacityExpanded) { voxelOpacityExpanded = !voxelOpacityExpanded }
            AnimatedVisibility(
                visible = voxelOpacityExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Global opacity slider
                    Text(
                        "Opacity: ${(voxelOpacity * 100).roundToInt()}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = voxelOpacity,
                        onValueChange = onVoxelOpacityChanged,
                        valueRange = 0.05f..1.0f
                    )

                    // Depth fade toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Depth Fade", style = MaterialTheme.typography.bodySmall)
                        Switch(
                            checked = depthFadeEnabled,
                            onCheckedChange = onDepthFadeEnabledChanged,
                            modifier = Modifier.height(24.dp)
                        )
                    }

                    if (depthFadeEnabled) {
                        // Reverse direction toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (depthFadeReversed) "Edges opaque → Center transparent"
                                else "Center opaque → Edges transparent",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Switch(
                                checked = depthFadeReversed,
                                onCheckedChange = onDepthFadeReversedChanged,
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }

                    // Layer slice
                    val effectiveMax = (gridDepth - 1).coerceAtLeast(1)
                    Text(
                        "Layer Slice: ${layerMin}–${layerMax.coerceAtMost(effectiveMax)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    RangeSlider(
                        value = layerMin.toFloat()..layerMax.coerceAtMost(effectiveMax).toFloat(),
                        onValueChange = { range ->
                            onLayerMinChanged(range.start.roundToInt())
                            onLayerMaxChanged(range.endInclusive.roundToInt())
                        },
                        valueRange = 0f..effectiveMax.toFloat(),
                        steps = (effectiveMax - 1).coerceAtLeast(0)
                    )
                }
            }

            HorizontalDivider()
        }

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
                    if (selectedLatticeType.is3D) {
                        SuggestionChip(
                            onClick = {
                                widthSlider = 20f; heightSlider = 20f; depthSlider = 20f
                                onGridSizeChanged(20, 20); onGridDepthChanged(20)
                            },
                            label = { Text("20³") }
                        )
                        SuggestionChip(
                            onClick = {
                                widthSlider = 30f; heightSlider = 30f; depthSlider = 30f
                                onGridSizeChanged(30, 30); onGridDepthChanged(30)
                            },
                            label = { Text("30³") }
                        )
                        SuggestionChip(
                            onClick = {
                                widthSlider = 50f; heightSlider = 50f; depthSlider = 50f
                                onGridSizeChanged(50, 50); onGridDepthChanged(50)
                            },
                            label = { Text("50³") }
                        )
                    } else {
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

                // Depth slider (3D only)
                if (selectedLatticeType.is3D) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Depth: ${depthSlider.roundToInt()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = depthSlider,
                        onValueChange = { depthSlider = it },
                        onValueChangeFinished = {
                            onGridDepthChanged(depthSlider.roundToInt())
                        },
                        valueRange = 5f..50f,
                        steps = 8
                    )
                }
            }
        }

        HorizontalDivider()

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
