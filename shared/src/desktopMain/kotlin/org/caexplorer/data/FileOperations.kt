package org.caexplorer.data

import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

actual fun saveSimulationFile(data: SimulationFileData) {
    val chooser = JFileChooser().apply {
        dialogTitle = "Save Simulation State"
        fileFilter = FileNameExtensionFilter("CA Explorer File (*.caex)", "caex")
        selectedFile = File("simulation.caex")
    }
    if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
        var file = chooser.selectedFile
        if (!file.name.endsWith(".caex")) file = File(file.absolutePath + ".caex")
        file.writeText(serializeSimulation(data))
    }
}

actual fun loadSimulationFile(): SimulationFileData? {
    val chooser = JFileChooser().apply {
        dialogTitle = "Load Simulation State"
        fileFilter = FileNameExtensionFilter("CA Explorer File (*.caex)", "caex")
    }
    if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return null
    val file = chooser.selectedFile ?: return null
    if (!file.exists()) return null
    return deserializeSimulation(file.readText())
}
