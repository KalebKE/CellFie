package org.caexplorer.ui.util

/**
 * Records simulation frames and exports them as an animated GIF.
 * Desktop has a full implementation; mobile platforms are no-op stubs.
 */
expect class GifRecorder() {
    var isRecording: Boolean
        private set
    var frameCount: Int
        private set
    /** 0.0–1.0 encoding progress during save */
    var saveProgress: Float
        private set
    /** True while encoding/saving is in progress */
    var isSaving: Boolean
        private set
    fun startRecording(width: Int, height: Int, delayMs: Int = 100)
    fun addFrame(cellColors: IntArray, gridWidth: Int, gridHeight: Int)
    fun stopAndSave()
    fun cancel()
}
