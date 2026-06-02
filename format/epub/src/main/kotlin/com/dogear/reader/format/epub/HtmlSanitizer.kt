package com.dogear.reader.format.epub

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import java.io.ByteArrayInputStream

/**
 * Strips active content from publisher XHTML before it is loaded into the reader WebView, and
 * parses leniently so real-world books with undeclared entities like `&nbsp;`, malformed markup,
 * or non-UTF-8 encodings still render instead of failing — the fault-tolerant approach calibre and
 * other readers use (Hardening research §10). The WebView runs JavaScript only for our injected
 * pager; removing author scripts/handlers here ensures no publisher script ever executes
 * (Security Review §A).
 *
 * Whitespace: jsoup's **HTML** tree-builder discards whitespace-only text nodes *at parse time*,
 * which can merge words across inline-tag boundaries (`it <em>would</em>` → "itwould"); this is not
 * recoverable with `prettyPrint(false)` (jsoup#1081). EPUB content documents are XHTML, so for
 * documents that actually declare themselves as XML/XHTML *and* don't rely on named HTML entities,
 * we parse with the **XML** parser, which preserves that whitespace. Anything else (loose HTML,
 * entity-heavy, or a failed/empty XML parse) falls back to the lenient HTML parser unchanged, so
 * the stricter parser can never regress a book.
 */
internal object HtmlSanitizer {

    /** Charset is auto-detected from the BOM / meta / XML declaration (defaults to UTF-8). */
    fun sanitize(bytes: ByteArray): String {
        if (looksLikeXml(bytes) && !hasNamedEntities(bytes)) {
            runCatching {
                cleanAndSerialize(Jsoup.parse(ByteArrayInputStream(bytes), null, "", Parser.xmlParser()))
            }.getOrNull()
                ?.takeIf { it.isNotBlank() && it != EMPTY }
                ?.let { return it }
        }
        return runCatching {
            cleanAndSerialize(Jsoup.parse(ByteArrayInputStream(bytes), null, ""))
        }.getOrDefault(EMPTY)
    }

    fun sanitize(html: String): String = runCatching {
        cleanAndSerialize(Jsoup.parse(html))
    }.getOrDefault(html)

    private fun cleanAndSerialize(doc: Document): String {
        doc.select("script, iframe, object, embed, link[rel=import]").remove()
        doc.allElements.forEach { el ->
            el.attributes().map { it.key }.forEach { key ->
                val lower = key.lowercase()
                val value = el.attr(key).trim()
                if (lower.startsWith("on")) el.removeAttr(key)
                if ((lower == "href" || lower == "src" || lower == "xlink:href") &&
                    value.startsWith("javascript:", ignoreCase = true)
                ) {
                    el.removeAttr(key)
                }
            }
        }
        doc.outputSettings().prettyPrint(false)
        return doc.html()
    }

    /** True if the document declares itself XML/XHTML (so the XML parser is appropriate). */
    private fun looksLikeXml(bytes: ByteArray): Boolean {
        val head = String(bytes, 0, minOf(bytes.size, 1024), Charsets.ISO_8859_1).lowercase()
        return head.contains("<?xml") || head.contains("xhtml")
    }

    /**
     * True if the bytes contain a named HTML entity outside XML's five built-ins (e.g. `&nbsp;`,
     * `&mdash;`). The XML parser can't resolve those, so such books must use the HTML parser.
     * Entity names are ASCII, so a byte-level scan is charset-independent.
     */
    private fun hasNamedEntities(bytes: ByteArray): Boolean {
        val text = String(bytes, Charsets.ISO_8859_1)
        return NAMED_ENTITY.containsMatchIn(text)
    }

    private val NAMED_ENTITY =
        Regex("&(?!(?:amp|lt|gt|quot|apos);|#)[a-zA-Z][a-zA-Z0-9]{1,31};")

    private const val EMPTY = "<html><body></body></html>"
}
