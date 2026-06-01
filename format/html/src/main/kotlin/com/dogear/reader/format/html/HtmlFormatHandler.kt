package com.dogear.reader.format.html

import com.dogear.reader.core.model.BookFormat
import com.dogear.reader.core.model.BookMetadata
import com.dogear.reader.format.api.BookFormatHandler
import com.dogear.reader.format.api.FileRef
import com.dogear.reader.format.api.ProbeResult
import com.dogear.reader.format.api.RawImage
import com.dogear.reader.format.api.content.BookContent
import com.dogear.reader.format.api.content.ReflowDocument
import com.dogear.reader.format.api.content.Resource
import com.dogear.reader.format.api.content.SpineItem
import com.dogear.reader.format.api.content.TocEntry
import org.jsoup.Jsoup
import javax.inject.Inject

/** Single-file HTML/HTM handler. Reflowed like EPUB after stripping active content. */
class HtmlFormatHandler @Inject constructor() : BookFormatHandler {

    override val supportedFormats: Set<BookFormat> = setOf(BookFormat.HTML)
    override val priority: Int = 70 // below FB2/signature handlers, above the text catch-all

    override suspend fun probe(ref: FileRef): ProbeResult {
        val ext = ref.extension?.lowercase()
        if (ext == "html" || ext == "htm") return ProbeResult.of(BookFormat.HTML)
        val header = runCatching {
            ref.openInputStream().use { input ->
                val buffer = ByteArray(2048)
                val read = input.read(buffer)
                if (read <= 0) "" else String(buffer, 0, read, Charsets.ISO_8859_1).lowercase()
            }
        }.getOrDefault("")
        return if (header.contains("<html") || header.contains("<!doctype html")) {
            ProbeResult.of(BookFormat.HTML)
        } else {
            ProbeResult.NoMatch
        }
    }

    override suspend fun extractMetadata(ref: FileRef): BookMetadata = runCatching {
        val title = Jsoup.parse(readText(ref)).title().takeIf { it.isNotBlank() }
        BookMetadata(title = title)
    }.getOrDefault(BookMetadata.Empty)

    override suspend fun extractCover(ref: FileRef): RawImage? = null

    override suspend fun openContent(ref: FileRef): BookContent = HtmlContent(readText(ref))

    private fun readText(ref: FileRef): String =
        ref.openInputStream().use { it.readBytes().toString(Charsets.UTF_8) }
}

/** Reflowable single HTML document with author scripts removed (Security Review §A). */
internal class HtmlContent(private val rawHtml: String) : BookContent.Reflowable {

    override suspend fun spine(): List<SpineItem> = listOf(SpineItem(0, "html", "text/html"))

    override suspend fun toc(): List<TocEntry> = emptyList()

    override suspend fun resource(path: String): Resource? = null

    override suspend fun document(spineIndex: Int): ReflowDocument {
        val doc = runCatching { Jsoup.parse(rawHtml) }.getOrNull()
        val sanitized = if (doc != null) {
            doc.select("script, iframe, object, embed").remove()
            doc.allElements.forEach { el ->
                el.attributes().map { it.key }.forEach { key ->
                    if (key.lowercase().startsWith("on")) el.removeAttr(key)
                }
            }
            doc.outputSettings().prettyPrint(false)
            doc.html()
        } else {
            "<html><body></body></html>"
        }
        return ReflowDocument(sanitized, "")
    }
}
