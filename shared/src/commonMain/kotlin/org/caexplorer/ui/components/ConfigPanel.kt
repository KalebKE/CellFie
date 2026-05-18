package org.caexplorer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Configuration panel for simulation settings.
 * Displayed as a side sheet or bottom sheet depending on layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigPanel(
    gridWidth: Int,
    gridHeight: Int,
    onGridSizeChanged: (width: Int, height: Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var widthSlider by remember(gridWidth) { mutableStateOf(gridWidth.toFloat()) }
    var heightSlider by remember(gridHeight) { mutableStateOf(gridHeight.toFloat()) }

    Card(
        modifier = modifier.width(280.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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

            // Grid width
            Text(
                "Grid Width: ${widthSlider.roundToInt()}",
                style = MaterialTheme.typography.labelMedium
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

            // Grid height
            Text(
                "Grid Height: ${heightSlider.roundToInt()}",
                style = MaterialTheme.typography.labelMedium
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

            HorizontalDivider()

            // Preset sizes
            Text(
                "Presets",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
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
}
