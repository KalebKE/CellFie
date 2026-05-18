package org.caexplorer.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.caexplorer.ui.screens.MainScreen
import org.caexplorer.ui.theme.CAExplorerTheme

/**
 * Shared action triggers for bridging desktop menu bar and MainScreen.
 */
object AppActions {
    var resetTrigger by mutableStateOf(0)
        private set
    var toggleGridTrigger by mutableStateOf(0)
        private set

    fun requestReset() { resetTrigger++ }
    fun requestToggleGrid() { toggleGridTrigger++ }
}

@Composable
fun App() {
    CAExplorerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            MainScreen()
        }
    }
}
