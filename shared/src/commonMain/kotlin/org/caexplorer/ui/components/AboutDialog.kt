package org.caexplorer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * About dialog displaying app information with a small Game of Life pattern as the icon.
 */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    // Glider + blinker pattern for the 8x8 "icon"
    val pattern = setOf(
        0 to 1, 1 to 2, 2 to 0, 2 to 1, 2 to 2,  // glider
        5 to 5, 5 to 6, 5 to 7                      // blinker
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Canvas(modifier = Modifier.size(80.dp)) {
                    val cellSize = size.width / 8f
                    // Background
                    drawRect(Color(0xFF1A1A2E), size = size)
                    // Live cells
                    for ((row, col) in pattern) {
                        drawRect(
                            color = Color(0xFF00D4AA),
                            topLeft = Offset(col * cellSize, row * cellSize),
                            size = Size(cellSize - 1f, cellSize - 1f)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "CA Explorer",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "Cellular Automaton Simulator",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Version 1.0.0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Built with Kotlin & Compose Multiplatform",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "github.com/KalebKE/CAExplorer",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        }
    )
}
