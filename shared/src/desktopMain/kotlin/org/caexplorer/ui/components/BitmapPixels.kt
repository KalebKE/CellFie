package org.caexplorer.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

/**
 * Desktop (JVM/Skia) implementation.
 * Creates an ImageBitmap by writing ARGB pixel data into a Skia Bitmap,
 * then converting to a Skia Image for Compose consumption.
 * This is extremely fast — direct memory copy, no per-pixel processing.
 */
actual fun createCellBitmap(colors: IntArray, width: Int, height: Int): ImageBitmap {
    val bitmap = Bitmap()
    bitmap.allocPixels(ImageInfo.makeN32(width, height, ColorAlphaType.PREMUL))

    // Convert ARGB IntArray to byte array in BGRA format (Skia N32 on little-endian)
    val pixelCount = width * height
    val bytes = ByteArray(pixelCount * 4)

    for (i in 0 until pixelCount) {
        val argb = if (i < colors.size) colors[i] else 0xFF000000.toInt()
        val a = (argb ushr 24) and 0xFF
        val r = (argb ushr 16) and 0xFF
        val g = (argb ushr 8) and 0xFF
        val b = argb and 0xFF
        val offset = i * 4
        bytes[offset] = b.toByte()
        bytes[offset + 1] = g.toByte()
        bytes[offset + 2] = r.toByte()
        bytes[offset + 3] = a.toByte()
    }

    bitmap.installPixels(bytes)
    // Convert Bitmap → Image → ComposeImageBitmap
    val image = Image.makeFromBitmap(bitmap)
    return image.toComposeImageBitmap()
}
