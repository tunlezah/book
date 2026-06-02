package com.dogear.reader.format.comic

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Size
import com.dogear.reader.format.api.content.BookContent
import com.dogear.reader.format.api.io.SafeZip
import java.io.File
import kotlin.math.max

/**
 * Comic (CBZ) content: a sequence of full-page images. Decodes one page at a time, downsampled
 * to the viewport size, so memory stays bounded regardless of book length (Reader Engineering
 * Research §3). Pages are in natural order via [SafeZip].
 */
internal class CbzContent(private val file: File) : BookContent.ImagePager {

    private val pages: List<String> by lazy {
        runCatching { SafeZip.entryNames(file).filter { isImage(it) } }.getOrDefault(emptyList())
    }

    override val pageCount: Int get() = pages.size

    override suspend fun page(index: Int, target: Size): Bitmap? {
        val name = pages.getOrNull(index) ?: return null
        val bytes = SafeZip.readEntry(file, name) ?: return null
        return decodeSampled(bytes, target)
    }

    private fun decodeSampled(bytes: ByteArray, target: Size): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val longSide = max(bounds.outWidth, bounds.outHeight)
        val targetMax = max(target.width, target.height).coerceAtLeast(1)
        if (longSide <= 0) return null
        var sample = 1
        while (longSide / (sample * 2) >= targetMax) sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) }.getOrNull()
    }

    private fun isImage(name: String): Boolean =
        name.substringAfterLast('.', "").lowercase() in setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")
}
