package com.dogear.reader.core.cover

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlin.math.max

/**
 * Decodes cover bytes downsampled to a bounded size, so we never load full-resolution cover
 * art into memory (Performance strategy: memory-aware decode). Uses `inSampleSize` to decode
 * at a power-of-two reduction first, which is cheap.
 */
internal object ThumbnailDecoder {

    fun decodeSampled(bytes: ByteArray, maxSide: Int): Bitmap? {
        if (bytes.isEmpty()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val srcMax = max(bounds.outWidth, bounds.outHeight)
        if (srcMax <= 0) return null

        var sample = 1
        while (srcMax / (sample * 2) >= maxSide) sample *= 2

        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565 // covers don't need alpha; halves memory
        }
        return runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) }.getOrNull()
    }
}
