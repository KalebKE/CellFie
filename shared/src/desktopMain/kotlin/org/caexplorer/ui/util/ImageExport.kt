package org.caexplorer.ui.util

import org.jetbrains.skia.*
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

actual fun exportImage(cellColors: IntArray, gridWidth: Int, gridHeight: Int) {
    if (gridWidth <= 0 || gridHeight <= 0 || cellColors.isEmpty()) return
    exportSimulationImage(cellColors, gridWidth, gridHeight)
}

private fun exportSimulationImage(
    cellColors: IntArray,
    gridWidth: Int,
    gridHeight: Int,
    scaleFactor: Int = 4
) {
    val imgWidth = gridWidth * scaleFactor
    val imgHeight = gridHeight * scaleFactor

    val bitmap = Bitmap()
    bitmap.allocPixels(ImageInfo.makeN32Premul(imgWidth, imgHeight))
    val canvas = Canvas(bitmap)

    for (row in 0 until gridHeight) {
        for (col in 0 until gridWidth) {
            val argb = cellColors[row * gridWidth + col]
            // Convert ARGB to Skia color (which is also ARGB on the API level)
            val paint = Paint().apply {
                color = argb
            }
            canvas.drawRect(
                Rect.makeXYWH(
                    (col * scaleFactor).toFloat(),
                    (row * scaleFactor).toFloat(),
                    scaleFactor.toFloat(),
                    scaleFactor.toFloat()
                ),
                paint
            )
        }
    }

    val chooser = JFileChooser().apply {
        dialogTitle = "Export Simulation Image"
        fileFilter = FileNameExtensionFilter("PNG Image", "png")
        selectedFile = File("ca_export.png")
    }

    if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
        var file = chooser.selectedFile
        if (!file.name.endsWith(".png")) file = File(file.absolutePath + ".png")

        val image = Image.makeFromBitmap(bitmap)
        val data = image.encodeToData(EncodedImageFormat.PNG) ?: return
        file.writeBytes(data.bytes)
    }
}
