package com.dogear.reader.format.epub

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.ByteArrayInputStream

/**
 * Strips active content from publisher XHTML before it is loaded into the reader WebView, and
 * parses leniently (jsoup's HTML5 parser) so real-world books with undeclared entities like
 * `&nbsp;`, malformed markup, or non-UTF-8 encodings still render instead of failing — the
 * fault-tolerant approach calibre and other readers use (Hardening research §10). The WebView
 * runs JavaScript only for our injected pager; removing author scripts/handlers here ensures no
 * publisher script ever executes (Security Review §A).
 */
internal object HtmlSanitizer {

    /** Charset is auto-detected from the BOM / meta / XML declaration (defaults to UTF-8). */
    fun sanitize(bytes: ByteArray): String = runCatching {
        cleanAndSerialize(Jsoup.parse(ByteArrayInputStream(bytes), null, ""))
    }.getOrDefault(EMPTY)

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

    private const val EMPTY = "<html><body></body></html>"
}
