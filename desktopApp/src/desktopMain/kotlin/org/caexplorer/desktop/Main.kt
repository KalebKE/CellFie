package org.caexplorer.desktop

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.caexplorer.ui.App
import org.caexplorer.ui.AppActions

fun main() {
    // Force dark appearance for the macOS native title bar
    System.setProperty("apple.awt.application.appearance", "NSAppearanceNameDarkAqua")

    application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "CellFie",
        icon = painterResource("icon.png"),
        state = rememberWindowState(width = 1280.dp, height = 900.dp)
    ) {
        MenuBar {
            Menu("File") {
                Item("New Simulation", onClick = { AppActions.requestReset() })
                Separator()
                Item("Save State...", onClick = { AppActions.requestSave() })
                Item("Load State...", onClick = { AppActions.requestLoad() })
                Separator()
                Item("Export Image...", onClick = { AppActions.requestExportImage() })
                Separator()
                Item("Record GIF...", onClick = { AppActions.requestStartGif() })
                Item("Stop Recording", onClick = { AppActions.requestStopGif() })
                Separator()
                Item("Exit", onClick = ::exitApplication)
            }
            Menu("Simulation") {
                Item("Play/Pause", onClick = { AppActions.requestTogglePlayPause() })
                Item("Step", onClick = { AppActions.requestStep() })
                Item("Rewind", onClick = { AppActions.requestRewind() })
                Separator()
                Item("Reset", onClick = { AppActions.requestReset() })
            }
            Menu("View") {
                Item("Toggle Grid", onClick = { AppActions.requestToggleGrid() })
                Item("Toggle Analysis", onClick = { AppActions.requestToggleAnalysis() })
                Item("Toggle Draw Mode", onClick = { AppActions.requestToggleDrawMode() })
                Separator()
                Item("Fit to Window", onClick = { AppActions.requestFitToWindow() })
            }
            Menu("Help") {
                Item("User Guide", onClick = { AppActions.requestHelp() })
                Item("Keyboard Shortcuts", onClick = { AppActions.requestKeyboardHelp() })
                Separator()
                Item("About CellFie", onClick = { AppActions.requestAbout() })
            }
        }
        App()
    }
    }
}
