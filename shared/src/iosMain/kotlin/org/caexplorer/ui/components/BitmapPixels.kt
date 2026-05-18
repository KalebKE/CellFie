package org.caexplorer.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig

/**
 * iOS implementation.
 * Creates an ImageBitmap from ARGB pixel data.
 * Uses Compose's built-in ImageBitmap with pixel buffer access via Skia (iOS uses Skia too in CMP).
 */
actual fun createCellBitmap(colors: IntArray, width: Int, height: Int): ImageBitmap {
    // On iOS with Compose Multiplatform, Skia is also the rendering backend.
    // We use the same Skia approach as desktop.
    val bitmap = org.jetbrains.skia.Bitmap()
    bitmap.allocPixels(
        org.jetbrains.skia.ImageInfo.makeN32(
            width, height,
            org.jetbrains.skia.ColorAlphaType.PREMUL
        )
    )

    val pixelCount = width * height
    val bytes = ByteArray(pixelCount * 4)

    for (i in 0 until pixelCount) {
        val argb = if (i < colors.size) colors[i] else 0xFF000000.toInt()
        val a = (argb ushr 24) and 0xFF
        val r = (argb ushr 16) and 0xFF
        val g = (argb ushr 8) and 0xFF
        val b = argb and 0xFF
        val offset = i * 4
        // BGRA format for Skia N32 on little-endian
        bytes[offset] = b.toByte()
        bytes[offset + 1] = g.toByte()
        bytes[offset + 2] = r.toByte()
        bytes[offset + 3] = a.toByte()
    }

    bitmap.installPixels(bytes)
    return bitmap.toComposeImageBitmap()
}

private fun org.jetbrains.skia.Bitmap.toComposeImageBitmap(): ImageBitmap {
    val image = org.jetbrains.skia.Image.makeFromBitmap(this)
    return image.toComposeImageBitmap()
}

private fun org.jetbrains.skia.Image.toComposeImageBitmap(): ImageBitmap {
    // This extension exists in Compose Multiplatform for iOS
    return androidx.compose.ui.graphics.toComposeImageBitmap(this)
}
