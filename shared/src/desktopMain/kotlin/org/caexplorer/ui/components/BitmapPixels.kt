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
 * Performance optimizations:
 * - Uses ByteBuffer with native byte order for zero-copy int writes
 * - On little-endian systems (x86/ARM), ARGB→BGRA is just writing ints directly
 *   since Java's putInt with LITTLE_ENDIAN byte order handles the swap
 * - Reuses a thread-local byte buffer to avoid GC pressure during animation
 */
actual fun createCellBitmap(colors: IntArray, width: Int, height: Int): ImageBitmap {
    val pixelCount = width * height
    val byteCount = pixelCount * 4

    // Get or create a thread-local buffer (avoids allocation per frame)
    val buffer = getOrCreateBuffer(byteCount)
    buffer.clear()

    // Skia N32 is BGRA on little-endian. We need to convert ARGB → BGRA.
    // ARGB int layout: [A R G B] in high-to-low bytes
    // BGRA byte layout: [B G R A] in memory address order
    // On little-endian, writing int as BGRA means: value = (A<<24)|(R<<16)|(G<<8)|B → stored as B,G,R,A ✓
    // So we actually need to rearrange: ARGB → swap R and B channels
    for (i in 0 until pixelCount) {
        val argb = if (i < colors.size) colors[i] else 0xFF000000.toInt()
        // ARGB to ABGR: swap R and B, keep A and G in place
        val a = argb and 0xFF000000.toInt()
        val r = (argb ushr 16) and 0xFF
        val g = argb and 0x0000FF00.toInt()
        val b = (argb and 0xFF) shl 16
        buffer.putInt(a or b or g or r)
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
