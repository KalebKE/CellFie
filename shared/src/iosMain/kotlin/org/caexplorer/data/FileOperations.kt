package org.caexplorer.data

actual fun saveSimulationFile(data: SimulationFileData) {
    // No-op on iOS for now
}

actual fun loadSimulationFile(): SimulationFileData? {
    // No-op on iOS for now
    return null
}
