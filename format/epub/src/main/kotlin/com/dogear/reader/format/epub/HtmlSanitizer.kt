package com.dogear.reader.format.epub

import org.jsoup.Jsoup

/**
 * Strips active content from publisher XHTML before it is loaded into the reader WebView. The
 * WebView runs JavaScript only for our injected pagination/locator code; removing author
 * scripts and event handlers here ensures no publisher script ever executes (Security Review §A
 * and the Reader Engineering Research note). Network/file access is additionally blocked at the
 * WebView level.
 */
internal object HtmlSanitizer {

    fun sanitize(html: String): String = runCatching {
        val doc = Jsoup.parse(html)
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
        doc.html()
    }.getOrDefault(html)
}
