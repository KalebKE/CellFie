package org.caexplorer.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.caexplorer.ui.screens.MainScreen
import org.caexplorer.ui.theme.CAExplorerTheme

@Composable
fun App() {
    CAExplorerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            MainScreen()
        }
    }
}
