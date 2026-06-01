package com.dogear.reader.format.comic

import com.dogear.reader.core.model.BookFormat
import com.dogear.reader.core.model.BookMetadata
import com.dogear.reader.format.api.BookFormatHandler
import com.dogear.reader.format.api.FileRef
import com.dogear.reader.format.api.ProbeResult
import com.dogear.reader.format.api.RawImage
import com.dogear.reader.format.api.content.BookContent
import com.dogear.reader.format.api.io.SafeZip
import com.dogear.reader.format.api.materialize
import java.io.File
import javax.inject.Inject

/**
 * CBZ (comic ZIP) handler. The cover is the first image in natural page order. CBR (RAR) is
 * intentionally gated behind a license review (junrar is GPL) and not included — see the
 * Format Support Report.
 */
class CbzFormatHandler @Inject constructor() : BookFormatHandler {

    override val supportedFormats: Set<BookFormat> = setOf(BookFormat.CBZ)
    override val priority: Int = 90 // below EPUB so an EPUB zip is never mistaken for a comic

    override suspend fun probe(ref: FileRef): ProbeResult {
        val file = runCatching { ref.materialize() }.getOrNull() ?: return ProbeResult.NoMatch
        if (!SafeZip.isZip(file)) return ProbeResult.NoMatch
        val isCbzExt = ref.extension?.equals("cbz", ignoreCase = true) == true
        return if (isCbzExt || hasImages(file)) ProbeResult.of(BookFormat.CBZ) else ProbeResult.NoMatch
    }

    override suspend fun extractMetadata(ref: FileRef): BookMetadata = BookMetadata.Empty

    override suspend fun openContent(ref: FileRef): BookContent = CbzContent(ref.materialize())

    override suspend fun extractCover(ref: FileRef): RawImage? {
        val file = ref.materialize()
        val first = SafeZip.readFirst(file) { isImage(it) } ?: return null
        return RawImage(first.bytes, mimeFor(first.name))
    }

    private fun hasImages(file: File): Boolean =
        runCatching { SafeZip.entryNames(file).any { isImage(it) } }.getOrDefault(false)

    private fun isImage(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in IMAGE_EXTENSIONS
    }

    private fun mimeFor(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        else -> "image/jpeg"
    }

    private companion object {
        val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")
    }
}
