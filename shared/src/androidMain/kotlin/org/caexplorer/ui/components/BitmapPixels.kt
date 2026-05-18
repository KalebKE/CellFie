package org.caexplorer.ui.components

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Android implementation.
 * Creates an ImageBitmap by writing ARGB pixel data into an Android Bitmap.
 */
actual fun createCellBitmap(colors: IntArray, width: Int, height: Int): ImageBitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val pixelCount = width * height
    val pixels = if (colors.size >= pixelCount) colors else {
        IntArray(pixelCount).also { colors.copyInto(it) }
    }
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap.asImageBitmap()
}
