package org.caexplorer.ui.util

import java.io.*
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

actual class GifRecorder {
    actual var isRecording: Boolean = false
        private set
    actual var frameCount: Int = 0
        private set

    private var frames = mutableListOf<IntArray>()
    private var gridWidth = 0
    private var gridHeight = 0
    private var delayMs = 100
    private val scaleFactor = 2

    actual fun startRecording(width: Int, height: Int, delayMs: Int) {
        this.gridWidth = width
        this.gridHeight = height
        this.delayMs = delayMs
        frames.clear()
        frameCount = 0
        isRecording = true
    }

    actual fun addFrame(cellColors: IntArray, gridWidth: Int, gridHeight: Int) {
        if (!isRecording) return
        frames.add(cellColors.copyOf())
        frameCount++
    }

    actual fun stopAndSave() {
        isRecording = false
        if (frames.isEmpty()) return

        val chooser = JFileChooser().apply {
            dialogTitle = "Save GIF Animation"
            fileFilter = FileNameExtensionFilter("GIF Animation", "gif")
            selectedFile = File("ca_animation.gif")
        }

        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            var file = chooser.selectedFile
            if (!file.name.endsWith(".gif")) file = File(file.absolutePath + ".gif")

            val imgWidth = gridWidth * scaleFactor
            val imgHeight = gridHeight * scaleFactor

            FileOutputStream(file).use { fos ->
                val encoder = SimpleGifEncoder(fos, imgWidth, imgHeight, delayMs / 10)
                for (frame in frames) {
                    val pixels = scaleUp(frame, gridWidth, gridHeight, scaleFactor)
                    encoder.addFrame(pixels, imgWidth, imgHeight)
                }
                encoder.finish()
            }
        }
        frames.clear()
    }

    actual fun cancel() {
        isRecording = false
        frames.clear()
        frameCount = 0
    }

    private fun scaleUp(colors: IntArray, w: Int, h: Int, scale: Int): IntArray {
        val outW = w * scale
        val outH = h * scale
        val out = IntArray(outW * outH)
        for (row in 0 until h) {
            for (col in 0 until w) {
                val color = colors[row * w + col]
                for (sy in 0 until scale) {
                    for (sx in 0 until scale) {
                        out[(row * scale + sy) * outW + (col * scale + sx)] = color
                    }
                }
            }
        }
        return out
    }
}

/**
 * Minimal GIF89a encoder. Writes an animated GIF with LZW compression
 * and a global 256-color palette.
 */
internal class SimpleGifEncoder(
    private val out: OutputStream,
    private val width: Int,
    private val height: Int,
    private val delayCentiseconds: Int
) {
    private var globalPalette: IntArray? = null
    private var firstFrame = true

    fun addFrame(pixels: IntArray, imgWidth: Int, imgHeight: Int) {
        val palette = buildPalette(pixels)
        val indexed = quantize(pixels, palette)

        if (firstFrame) {
            globalPalette = palette
            writeHeader(palette)
            writeNetscapeExtension()
            firstFrame = false
        }

        writeGraphicControlExtension(delayCentiseconds)
        writeImageDescriptor(imgWidth, imgHeight)
        writeLzwCompressed(indexed, 8)
    }

    fun finish() {
        out.write(0x3B) // GIF trailer
        out.flush()
    }

    private fun buildPalette(pixels: IntArray): IntArray {
        val colors = LinkedHashSet<Int>(512)
        for (px in pixels) {
            val rgb = px and 0x00FFFFFF
            colors.add(rgb)
            if (colors.size >= 256) break
        }
        val palette = IntArray(256)
        var i = 0
        for (c in colors) {
            palette[i++] = c
            if (i >= 256) break
        }
        return palette
    }

    private fun quantize(pixels: IntArray, palette: IntArray): ByteArray {
        val paletteSize = palette.count { it != 0 || palette[0] == 0 }.coerceAtMost(256)
        val lookup = HashMap<Int, Byte>(paletteSize * 2)
        for (i in 0 until paletteSize.coerceAtMost(256)) {
            lookup[palette[i]] = i.toByte()
        }

        val result = ByteArray(pixels.size)
        for (i in pixels.indices) {
            val rgb = pixels[i] and 0x00FFFFFF
            val idx = lookup[rgb]
            if (idx != null) {
                result[i] = idx
            } else {
                result[i] = findNearest(rgb, palette, paletteSize)
            }
        }
        return result
    }

    private fun findNearest(rgb: Int, palette: IntArray, size: Int): Byte {
        val r = (rgb shr 16) and 0xFF
        val g = (rgb shr 8) and 0xFF
        val b = rgb and 0xFF
        var bestIdx = 0
        var bestDist = Int.MAX_VALUE
        for (i in 0 until size.coerceAtMost(256)) {
            val pr = (palette[i] shr 16) and 0xFF
            val pg = (palette[i] shr 8) and 0xFF
            val pb = palette[i] and 0xFF
            val dist = (r - pr) * (r - pr) + (g - pg) * (g - pg) + (b - pb) * (b - pb)
            if (dist < bestDist) {
                bestDist = dist
                bestIdx = i
            }
        }
        return bestIdx.toByte()
    }

    private fun writeHeader(palette: IntArray) {
        // GIF89a signature
        out.write("GIF89a".toByteArray(Charsets.US_ASCII))
        // Logical Screen Descriptor
        writeShort(width)
        writeShort(height)
        // GCT flag=1, color resolution=7 (8 bits), sort=0, GCT size=7 (256 colors)
        out.write(0xF7)
        out.write(0) // background color index
        out.write(0) // pixel aspect ratio
        // Global Color Table (256 * 3 bytes)
        for (i in 0 until 256) {
            val c = palette[i]
            out.write((c shr 16) and 0xFF) // R
            out.write((c shr 8) and 0xFF)  // G
            out.write(c and 0xFF)           // B
        }
    }

    private fun writeNetscapeExtension() {
        // Application Extension for looping
        out.write(0x21) // Extension introducer
        out.write(0xFF) // Application extension
        out.write(11)   // Block size
        out.write("NETSCAPE2.0".toByteArray(Charsets.US_ASCII))
        out.write(3)    // Sub-block size
        out.write(1)    // Sub-block ID
        writeShort(0)   // Loop count (0 = infinite)
        out.write(0)    // Block terminator
    }

    private fun writeGraphicControlExtension(delay: Int) {
        out.write(0x21) // Extension introducer
        out.write(0xF9) // Graphic Control Extension
        out.write(4)    // Block size
        out.write(0x00) // Packed byte: disposal=none, no user input, no transparency
        writeShort(delay)
        out.write(0)    // Transparent color index
        out.write(0)    // Block terminator
    }

    private fun writeImageDescriptor(imgWidth: Int, imgHeight: Int) {
        out.write(0x2C) // Image separator
        writeShort(0)   // Left
        writeShort(0)   // Top
        writeShort(imgWidth)
        writeShort(imgHeight)
        out.write(0)    // Packed byte: no local color table, not interlaced
    }

    private fun writeLzwCompressed(pixels: ByteArray, minCodeSize: Int) {
        out.write(minCodeSize)

        val clearCode = 1 shl minCodeSize
        val eoiCode = clearCode + 1

        val buf = ByteArrayOutputStream(pixels.size)
        val writer = BitWriter(buf)

        var codeSize = minCodeSize + 1
        var nextCode = eoiCode + 1
        val maxTableSize = 4096

        // String table: maps prefix<<8|suffix to code
        val table = HashMap<Long, Int>(maxTableSize * 2)

        fun resetTable() {
            table.clear()
            for (i in 0 until clearCode) {
                table[i.toLong()] = i
            }
            codeSize = minCodeSize + 1
            nextCode = eoiCode + 1
        }

        // Write initial clear code
        writer.writeBits(clearCode, codeSize)
        resetTable()

        if (pixels.isEmpty()) {
            writer.writeBits(eoiCode, codeSize)
            writer.flush()
            writeSubBlocks(buf.toByteArray())
            return
        }

        var prefix = (pixels[0].toInt() and 0xFF)

        for (i in 1 until pixels.size) {
            val suffix = pixels[i].toInt() and 0xFF
            val key = (prefix.toLong() shl 12) or suffix.toLong()

            val existing = table[key]
            if (existing != null) {
                prefix = existing
            } else {
                writer.writeBits(prefix, codeSize)

                if (nextCode < maxTableSize) {
                    table[key] = nextCode++
                    if (nextCode > (1 shl codeSize) && codeSize < 12) {
                        codeSize++
                    }
                } else {
                    writer.writeBits(clearCode, codeSize)
                    resetTable()
                }
                prefix = suffix
            }
        }

        writer.writeBits(prefix, codeSize)
        writer.writeBits(eoiCode, codeSize)
        writer.flush()

        writeSubBlocks(buf.toByteArray())
    }

    private fun writeSubBlocks(data: ByteArray) {
        var offset = 0
        while (offset < data.size) {
            val blockSize = minOf(255, data.size - offset)
            out.write(blockSize)
            out.write(data, offset, blockSize)
            offset += blockSize
        }
        out.write(0) // Block terminator
    }

    private fun writeShort(value: Int) {
        out.write(value and 0xFF)
        out.write((value shr 8) and 0xFF)
    }
}

/**
 * Writes individual bits to an output stream, packing them LSB-first.
 */
private class BitWriter(private val out: ByteArrayOutputStream) {
    private var accumulator = 0
    private var bitCount = 0

    fun writeBits(value: Int, numBits: Int) {
        var v = value
        var remaining = numBits
        while (remaining > 0) {
            accumulator = accumulator or ((v and 1) shl bitCount)
            bitCount++
            v = v shr 1
            remaining--
            if (bitCount == 8) {
                out.write(accumulator)
                accumulator = 0
                bitCount = 0
            }
        }
    }

    fun flush() {
        if (bitCount > 0) {
            out.write(accumulator)
            accumulator = 0
            bitCount = 0
        }
    }
}
