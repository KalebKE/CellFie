package org.caexplorer.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.caexplorer.data.AppSettings
import org.caexplorer.data.SettingsKeys
import org.caexplorer.ui.screens.MainScreen
import org.caexplorer.ui.theme.AppPalette
import org.caexplorer.ui.theme.CAExplorerTheme

/**
 * Shared action triggers for bridging desktop menu bar and MainScreen.
 */
object AppActions {
    var resetTrigger by mutableStateOf(0)
        private set
    var toggleGridTrigger by mutableStateOf(0)
        private set
    var exportImageTrigger by mutableStateOf(0)
        private set
    var startGifTrigger by mutableStateOf(0)
        private set
    var stopGifTrigger by mutableStateOf(0)
        private set
    var togglePlayPauseTrigger by mutableStateOf(0)
        private set
    var stepTrigger by mutableStateOf(0)
        private set
    var rewindTrigger by mutableStateOf(0)
        private set
    var toggleAnalysisTrigger by mutableStateOf(0)
        private set
    var toggleDrawModeTrigger by mutableStateOf(0)
        private set
    var fitToWindowTrigger by mutableStateOf(0)
        private set
    var aboutTrigger by mutableStateOf(0)
        private set
    var keyboardHelpTrigger by mutableStateOf(0)
        private set
    var helpTrigger by mutableStateOf(0)
        private set
    // Save/load triggers — may also be wired by save/load feature
    var saveTrigger by mutableStateOf(0)
        private set
    var loadTrigger by mutableStateOf(0)
        private set

    fun requestReset() { resetTrigger++ }
    fun requestToggleGrid() { toggleGridTrigger++ }
    fun requestExportImage() { exportImageTrigger++ }
    fun requestStartGif() { startGifTrigger++ }
    fun requestStopGif() { stopGifTrigger++ }
    fun requestTogglePlayPause() { togglePlayPauseTrigger++ }
    fun requestStep() { stepTrigger++ }
    fun requestRewind() { rewindTrigger++ }
    fun requestToggleAnalysis() { toggleAnalysisTrigger++ }
    fun requestToggleDrawMode() { toggleDrawModeTrigger++ }
    fun requestFitToWindow() { fitToWindowTrigger++ }
    fun requestAbout() { aboutTrigger++ }
    fun requestKeyboardHelp() { keyboardHelpTrigger++ }
    fun requestHelp() { helpTrigger++ }
    fun requestSave() { saveTrigger++ }
    fun requestLoad() { loadTrigger++ }
}

@Composable
fun App() {
    val savedPaletteName = AppSettings.getString(SettingsKeys.APP_PALETTE, AppPalette.FIRE.name)
    var palette by remember {
        mutableStateOf(
            AppPalette.entries.find { it.name == savedPaletteName } ?: AppPalette.FIRE
        )
    }

    CAExplorerTheme(palette = palette) {
        Surface(modifier = Modifier.fillMaxSize()) {
            MainScreen(
                selectedPalette = palette,
                onPaletteChanged = { newPalette ->
                    palette = newPalette
                    AppSettings.putString(SettingsKeys.APP_PALETTE, newPalette.name)
                }
            )
        }
    }
}
