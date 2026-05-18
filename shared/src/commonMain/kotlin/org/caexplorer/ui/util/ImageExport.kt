package org.caexplorer.ui.util

/**
 * Export the current simulation state as a PNG image.
 * Desktop: opens a file chooser and saves as PNG.
 * Other platforms: no-op stub.
 */
expect fun exportImage(cellColors: IntArray, gridWidth: Int, gridHeight: Int)
