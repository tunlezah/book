package com.dogear.reader.format.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Size
import com.dogear.reader.format.api.content.BookContent
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import kotlin.math.min

/**
 * Fixed-page PDF content. Per the research, [PdfRenderer] is NOT thread-safe and only one page
 * may be open at a time, so every open/render/close is serialized behind a [Mutex]. Pages are
 * rendered at view resolution (bounded), filled white first (PDFs assume an opaque page), and
 * the bitmap is owned by the caller (the engine recycles it). The renderer stays open for the
 * reading session and is released in [close].
 */
internal class PdfContent(file: File) : BookContent.FixedPage {

    private val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = PdfRenderer(descriptor)
    private val lock = Mutex()

    override val pageCount: Int get() = renderer.pageCount

    override suspend fun renderPage(index: Int, target: Size): Bitmap? = lock.withLock {
        if (index < 0 || index >= renderer.pageCount) return null
        runCatching {
            renderer.openPage(index).use { page ->
                val scale = min(
                    target.width.toFloat() / page.width.coerceAtLeast(1),
                    target.height.toFloat() / page.height.coerceAtLeast(1),
                ).coerceAtMost(MAX_SCALE)
                val width = (page.width * scale).toInt().coerceAtLeast(1)
                val height = (page.height * scale).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                Canvas(bitmap).drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            }
        }.getOrNull()
    }

    override fun close() {
        runCatching { renderer.close() }
        runCatching { descriptor.close() }
    }

    private companion object {
        const val MAX_SCALE = 3f
    }
}
