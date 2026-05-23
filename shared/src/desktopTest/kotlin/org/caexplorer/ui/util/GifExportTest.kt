package org.caexplorer.ui.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.io.ByteArrayOutputStream

class GifExportTest {

    @Test
    fun `medianCut produces diverse palette from CA-like colors`() {
        val colors = mutableSetOf<Int>()
        for (i in 0 until 600) {
            val t = i.toDouble() / 600
            val r = (10 + t * 245).toInt().coerceIn(0, 255)
            val g = (10 + t * 245 + (i % 7) - 3).toInt().coerceIn(0, 255)
            val b = (3 + t * 15).toInt().coerceIn(0, 255)
            colors.add((r shl 16) or (g shl 8) or b)
        }
        val colorArray = colors.toIntArray()
        assertTrue(colorArray.size > 256, "Need > 256 unique colors, got ${colorArray.size}")

        val palette = GifRecorder.medianCut(colorArray, 256)

        val uniquePalette = HashSet<Int>()
        for (c in palette) uniquePalette.add(c)
        assertTrue(uniquePalette.size > 100,
            "Palette should be diverse, got only ${uniquePalette.size} unique entries")

        // Verify palette spans the input range
        val minR = palette.filter { it != 0 }.minOf { (it shr 16) and 0xFF }
        val maxR = palette.maxOf { (it shr 16) and 0xFF }
        assertTrue(maxR - minR > 100,
            "Palette R range should be wide, got $minR..$maxR")
    }

    @Test
    fun `medianCut handles few colors correctly`() {
        val colors = intArrayOf(0xFF0000, 0x00FF00, 0x0000FF, 0xFFFF00, 0xFF00FF)
        val palette = GifRecorder.medianCut(colors, 256)

        // With only 5 input colors, should get 5 non-trivial buckets
        val nonZero = palette.count { it != 0 }
        assertTrue(nonZero >= 5, "Expected at least 5 palette entries, got $nonZero")
    }

    @Test
    fun `GIF encoder produces valid file with diverse colors`() {
        val width = 10
        val height = 10
        val baos = ByteArrayOutputStream()
        val encoder = SimpleGifEncoder(baos, width, height, 10)

        // Build a diverse palette
        val palette = IntArray(256)
        for (i in 0 until 256) {
            palette[i] = ((i * 37) % 256 shl 16) or ((i * 73) % 256 shl 8) or ((i * 113) % 256)
        }
        encoder.setGlobalPalette(palette)

        // Create a frame with diverse pixel values (ARGB format)
        val pixels = IntArray(width * height) { i ->
            val idx = i % 256
            0xFF000000.toInt() or palette[idx]
        }
        encoder.addFrame(pixels, width, height)
        encoder.finish()

        val data = baos.toByteArray()

        // Verify GIF header
        assertEquals('G'.code.toByte(), data[0])
        assertEquals('I'.code.toByte(), data[1])
        assertEquals('F'.code.toByte(), data[2])

        // Verify GIF trailer
        assertEquals(0x3B.toByte(), data[data.size - 1])

        // Verify palette diversity in GCT
        val gctStart = 13
        val paletteColors = mutableSetOf<Int>()
        for (i in 0 until 256) {
            val r = data[gctStart + i * 3].toInt() and 0xFF
            val g = data[gctStart + i * 3 + 1].toInt() and 0xFF
            val b = data[gctStart + i * 3 + 2].toInt() and 0xFF
            paletteColors.add((r shl 16) or (g shl 8) or b)
        }
        assertTrue(paletteColors.size > 100, "Palette should have diverse colors, got ${paletteColors.size}")
    }

    @Test
    fun `LZW encoder handles all-zero pixels correctly`() {
        val width = 100
        val height = 100
        val baos = ByteArrayOutputStream()
        val encoder = SimpleGifEncoder(baos, width, height, 10)

        val palette = IntArray(256)
        palette[0] = 0xFF0000 // red
        palette[1] = 0x00FF00 // green
        palette[2] = 0x0000FF // blue
        encoder.setGlobalPalette(palette)

        val pixels = IntArray(width * height) { 0xFF000000.toInt() or 0xFF0000 }
        encoder.addFrame(pixels, width, height)
        encoder.finish()

        val data = baos.toByteArray()

        assertEquals(0x3B.toByte(), data[data.size - 1])
        assertTrue(data.size > 100, "GIF should be larger than 100 bytes for ${width * height} pixels, got ${data.size}")

        val gctStart = 13
        assertEquals(0xFF, data[gctStart].toInt() and 0xFF)
        assertEquals(0x00, data[gctStart + 1].toInt() and 0xFF)
        assertEquals(0x00, data[gctStart + 2].toInt() and 0xFF)
    }
}
