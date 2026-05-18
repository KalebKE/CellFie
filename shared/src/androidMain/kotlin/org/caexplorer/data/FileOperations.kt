package org.caexplorer.data

actual fun saveSimulationFile(data: SimulationFileData) {
    // No-op on Android for now
}

actual fun loadSimulationFile(): SimulationFileData? {
    // No-op on Android for now
    return null
}
