package org.caexplorer.ui.util

actual class GifRecorder {
    actual var isRecording: Boolean = false
        private set
    actual var frameCount: Int = 0
        private set
    actual var saveProgress: Float = 0f
        private set
    actual var isSaving: Boolean = false
        private set
    actual fun startRecording(width: Int, height: Int, delayMs: Int) {}
    actual fun addFrame(cellColors: IntArray, gridWidth: Int, gridHeight: Int) {}
    actual fun stopAndSave() {}
    actual fun cancel() {}
}
