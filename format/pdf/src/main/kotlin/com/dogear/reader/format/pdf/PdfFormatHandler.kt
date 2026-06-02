package com.dogear.reader.format.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.dogear.reader.core.model.BookFormat
import com.dogear.reader.core.model.BookMetadata
import com.dogear.reader.format.api.BookFormatHandler
import com.dogear.reader.format.api.FileRef
import com.dogear.reader.format.api.ProbeResult
import com.dogear.reader.format.api.RawImage
import com.dogear.reader.format.api.content.BookContent
import com.dogear.reader.format.api.io.Magic
import com.dogear.reader.format.api.materialize
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlin.math.max

/**
 * PDF handler using the platform [PdfRenderer] (sandboxed, no AGPL — Format Support + Security
 * reviews). The cover is the first page rendered at a bounded resolution; PDF metadata isn't
 * exposed by PdfRenderer, so the importer derives the title from the filename.
 */
class PdfFormatHandler @Inject constructor() : BookFormatHandler {

    override val supportedFormats: Set<BookFormat> = setOf(BookFormat.PDF)
    override val priority: Int = 100

    override suspend fun probe(ref: FileRef): ProbeResult =
        if (Magic.isPdf(Magic.header(ref, 5))) ProbeResult.of(BookFormat.PDF) else ProbeResult.NoMatch

    override suspend fun extractMetadata(ref: FileRef): BookMetadata = BookMetadata.Empty

    override suspend fun openContent(ref: FileRef): BookContent = PdfContent(ref.materialize())

    override suspend fun extractCover(ref: FileRef): RawImage? = runCatching {
        val file = ref.materialize()
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                if (renderer.pageCount == 0) return null
                renderer.openPage(0).use { page ->
                    val scale = MAX_SIDE.toFloat() / max(page.width, page.height).coerceAtLeast(1)
                    val width = (page.width * scale).toInt().coerceAtLeast(1)
                    val height = (page.height * scale).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    // PDFs assume a white page; fill before rendering to avoid transparency.
                    Canvas(bitmap).drawColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    val bytes = ByteArrayOutputStream().use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                        out.toByteArray()
                    }
                    bitmap.recycle()
                    RawImage(bytes, "image/png")
                }
            }
        }
    }.getOrNull()

    private companion object {
        const val MAX_SIDE = 1024
    }
}
