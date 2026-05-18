package org.caexplorer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.caexplorer.domain.colorscheme.ColorScheme as CAColorScheme
import org.caexplorer.ui.screens.InitPattern
import kotlin.math.roundToInt

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
    onResetSimulation: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var widthSlider by remember(gridWidth) { mutableStateOf(gridWidth.toFloat()) }
    var heightSlider by remember(gridHeight) { mutableStateOf(gridHeight.toFloat()) }

    Card(
        modifier = modifier.width(300.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
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
            Text(
                "Color Scheme",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
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

            HorizontalDivider()

            // --- Speed Control ---
            Text(
                "Speed",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
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

            HorizontalDivider()

            // --- Initialization Pattern ---
            Text(
                "Init Pattern",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
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

            HorizontalDivider()

            // --- Grid Size ---
            Text(
                "Grid Size",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

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
}
