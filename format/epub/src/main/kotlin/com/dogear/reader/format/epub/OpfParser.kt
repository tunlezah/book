package com.dogear.reader.format.epub

import android.util.Xml
import com.dogear.reader.core.model.BookMetadata
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream

/**
 * Minimal, defensive OPF/container parser. Uses the platform pull parser (KXmlParser), which
 * does not expand external entities, and we disable DOCTYPE processing — closing the
 * "billion laughs" XML-entity-expansion vector (Security Review §D).
 */
internal object OpfParser {

    data class Opf(val metadata: BookMetadata, val coverHref: String?)

    /** Returns the OPF rootfile path from META-INF/container.xml, or null. */
    fun parseContainer(bytes: ByteArray): String? = runCatching {
        val parser = newParser(bytes)
        var path: String? = null
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "rootfile") {
                path = parser.getAttributeValue(null, "full-path")
                if (path != null) break
            }
            event = parser.next()
        }
        path
    }.getOrNull()

    fun parseOpf(bytes: ByteArray): Opf = runCatching {
        val parser = newParser(bytes)
        var title: String? = null
        var subtitle: String? = null
        val authors = mutableListOf<String>()
        var publisher: String? = null
        var date: String? = null
        var description: String? = null
        var isbn: String? = null
        var language: String? = null
        val subjects = mutableListOf<String>()
        var metaCoverId: String? = null
        val manifest = HashMap<String, ManifestItem>()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "title" -> title = title ?: parser.nextTextSafe()
                    "creator" -> parser.nextTextSafe()?.let { authors += it }
                    "publisher" -> publisher = publisher ?: parser.nextTextSafe()
                    "date" -> date = date ?: parser.nextTextSafe()
                    "description" -> description = description ?: parser.nextTextSafe()
                    "language" -> language = language ?: parser.nextTextSafe()
                    "subject" -> parser.nextTextSafe()?.let { subjects += it }
                    "identifier" -> {
                        val scheme = parser.getAttributeValue(null, "scheme")
                        val value = parser.nextTextSafe()
                        if (value != null && looksLikeIsbn(scheme, value)) isbn = isbn ?: value
                    }
                    "meta" -> {
                        if (parser.getAttributeValue(null, "name") == "cover") {
                            metaCoverId = parser.getAttributeValue(null, "content")
                        }
                    }
                    "item" -> {
                        val id = parser.getAttributeValue(null, "id")
                        val href = parser.getAttributeValue(null, "href")
                        if (id != null && href != null) {
                            manifest[id] = ManifestItem(
                                href = href,
                                mediaType = parser.getAttributeValue(null, "media-type"),
                                properties = parser.getAttributeValue(null, "properties"),
                            )
                        }
                    }
                }
            }
            event = parser.next()
        }

        val coverHref = resolveCover(metaCoverId, manifest)
        Opf(
            metadata = BookMetadata(
                title = title?.trim(),
                subtitle = subtitle?.trim(),
                authors = authors.map { it.trim() }.filter { it.isNotEmpty() },
                publisher = publisher?.trim(),
                publishedDate = date?.trim(),
                description = description?.trim(),
                isbn = isbn?.trim(),
                language = language?.trim(),
                categories = subjects.map { it.trim() }.filter { it.isNotEmpty() },
            ),
            coverHref = coverHref,
        )
    }.getOrDefault(Opf(BookMetadata.Empty, null))

    private fun resolveCover(metaCoverId: String?, manifest: Map<String, ManifestItem>): String? {
        metaCoverId?.let { id -> manifest[id]?.href?.let { return it } }
        // EPUB3 cover-image property
        manifest.values.firstOrNull { it.properties?.contains("cover-image") == true }
            ?.let { return it.href }
        // Fallback: an item that is an image and whose id/href hints "cover"
        return manifest.values.firstOrNull {
            it.mediaType?.startsWith("image/") == true && it.href.contains("cover", ignoreCase = true)
        }?.href
    }

    private data class ManifestItem(val href: String, val mediaType: String?, val properties: String?)

    private fun looksLikeIsbn(scheme: String?, value: String): Boolean {
        if (scheme?.contains("ISBN", ignoreCase = true) == true) return true
        val digits = value.filter { it.isDigit() }
        return digits.length == 10 || digits.length == 13
    }

    private fun newParser(bytes: ByteArray): XmlPullParser = Xml.newPullParser().apply {
        setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        runCatching { setFeature("http://xmlpull.org/v1/doc/features.html#process-docdecl", false) }
        setInput(ByteArrayInputStream(bytes), null)
    }

    private fun XmlPullParser.nextTextSafe(): String? =
        runCatching { nextText().takeIf { it.isNotBlank() } }.getOrNull()
}
