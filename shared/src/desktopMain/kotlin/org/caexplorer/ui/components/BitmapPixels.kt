package org.caexplorer.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Desktop (JVM/Skia) implementation.
 * Creates an ImageBitmap by writing ARGB pixel data into a Skia Bitmap,
 * then converting to a Skia Image for Compose consumption.
 *
 * Skia N32 is BGRA_8888 on little-endian systems. An ARGB packed int
 * (0xAARRGGBB) stored via putInt with LITTLE_ENDIAN byte order produces
 * memory bytes [BB, GG, RR, AA] which IS the BGRA byte layout Skia expects.
 * No channel swapping is needed.
 */
actual fun createCellBitmap(colors: IntArray, width: Int, height: Int): ImageBitmap {
    val pixelCount = width * height
    val byteCount = pixelCount * 4

    val buffer = getOrCreateBuffer(byteCount)
    buffer.clear()

    for (i in 0 until pixelCount) {
        val argb = if (i < colors.size) colors[i] else 0xFF000000.toInt()
        buffer.putInt(argb)
    }

    val bitmap = Bitmap()
    bitmap.allocPixels(ImageInfo.makeN32(width, height, ColorAlphaType.PREMUL))
    bitmap.installPixels(buffer.array())

    val image = Image.makeFromBitmap(bitmap)
    return image.toComposeImageBitmap()
}

// Thread-local buffer cache to avoid GC pressure during animation
private val bufferCache = ThreadLocal<ByteBuffer>()

private fun getOrCreateBuffer(minSize: Int): ByteBuffer {
    val existing = bufferCache.get()
    if (existing != null && existing.capacity() >= minSize) return existing
    val newBuffer = ByteBuffer.allocate(minSize).order(ByteOrder.LITTLE_ENDIAN)
    bufferCache.set(newBuffer)
    return newBuffer
}
