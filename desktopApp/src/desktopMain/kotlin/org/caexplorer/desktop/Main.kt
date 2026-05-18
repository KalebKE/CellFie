package org.caexplorer.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.caexplorer.ui.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "CA Explorer",
        state = rememberWindowState(width = 1280.dp, height = 900.dp)
    ) {
        App()
    }
}
