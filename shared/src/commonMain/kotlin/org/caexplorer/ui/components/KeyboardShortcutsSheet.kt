package org.caexplorer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/**
 * Dialog listing all keyboard shortcuts in a clean table format.
 */
@Composable
fun KeyboardShortcutsSheet(onDismiss: () -> Unit) {
    val shortcuts = listOf(
        "Space" to "Play / Pause",
        "S" to "Step one generation",
        "R" to "Rewind one generation",
        "G" to "Toggle grid overlay",
        "F" to "Fit to window",
        "D" to "Toggle draw mode",
        "A" to "Toggle analysis panel",
        "E" to "Export image (PNG)",
        "+/-" to "Adjust speed",
        "Ctrl+S" to "Save state",
        "Ctrl+O" to "Load state",
        "?" to "Show this help",
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        title = {
            Text("Keyboard Shortcuts", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .widthIn(min = 300.dp)
            ) {
                shortcuts.forEach { (key, description) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(80.dp)
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (key != shortcuts.last().first) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    )
}
