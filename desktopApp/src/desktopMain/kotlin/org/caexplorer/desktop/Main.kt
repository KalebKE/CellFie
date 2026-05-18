package org.caexplorer.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.caexplorer.ui.App
import org.caexplorer.ui.AppActions

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "CA Explorer",
        state = rememberWindowState(width = 1280.dp, height = 900.dp)
    ) {
        MenuBar {
            Menu("File") {
                Item("New Simulation", onClick = { AppActions.requestReset() })
                Item("Export Image...", onClick = { AppActions.requestExportImage() })
                Separator()
                Item("Record GIF...", onClick = { AppActions.requestStartGif() })
                Item("Stop Recording", onClick = { AppActions.requestStopGif() })
                Separator()
                Item("Exit", onClick = ::exitApplication)
            }
            Menu("View") {
                Item("Toggle Grid", onClick = { AppActions.requestToggleGrid() })
                Item("Zoom In", onClick = {})
                Item("Zoom Out", onClick = {})
                Item("Reset Zoom", onClick = {})
            }
            Menu("Help") {
                Item("About CA Explorer", onClick = {})
            }
        }
        App()
    }
}
