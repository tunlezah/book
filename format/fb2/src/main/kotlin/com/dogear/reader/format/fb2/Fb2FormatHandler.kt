package com.dogear.reader.format.fb2

import com.dogear.reader.core.model.BookFormat
import com.dogear.reader.core.model.BookMetadata
import com.dogear.reader.format.api.BookFormatHandler
import com.dogear.reader.format.api.FileRef
import com.dogear.reader.format.api.ProbeResult
import com.dogear.reader.format.api.RawImage
import com.dogear.reader.format.api.content.BookContent
import javax.inject.Inject

/** FictionBook (FB2) handler — a single XML document, reflowed like EPUB. */
class Fb2FormatHandler @Inject constructor() : BookFormatHandler {

    override val supportedFormats: Set<BookFormat> = setOf(BookFormat.FB2)
    override val priority: Int = 80 // above the text catch-all, below signature-based handlers

    override suspend fun probe(ref: FileRef): ProbeResult {
        if (ref.extension?.equals("fb2", ignoreCase = true) == true) return ProbeResult.of(BookFormat.FB2)
        val header = runCatching {
            ref.openInputStream().use { input ->
                val buffer = ByteArray(2048)
                val read = input.read(buffer)
                if (read <= 0) "" else String(buffer, 0, read, Charsets.ISO_8859_1)
            }
        }.getOrDefault("")
        return if (header.contains("FictionBook")) ProbeResult.of(BookFormat.FB2) else ProbeResult.NoMatch
    }

    override suspend fun extractMetadata(ref: FileRef): BookMetadata =
        runCatching { Fb2Parser.metadata(Fb2Parser.parse(readBytes(ref))) }.getOrDefault(BookMetadata.Empty)

    override suspend fun extractCover(ref: FileRef): RawImage? = runCatching {
        val doc = Fb2Parser.parse(readBytes(ref))
        val id = Fb2Parser.coverId(doc) ?: return null
        val (data, mime) = Fb2Parser.binary(doc, id) ?: return null
        RawImage(data, mime)
    }.getOrNull()

    override suspend fun openContent(ref: FileRef): BookContent = Fb2Content(readBytes(ref))

    private fun readBytes(ref: FileRef): ByteArray = ref.openInputStream().use { it.readBytes() }
}
