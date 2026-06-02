package com.dogear.reader.format.epub

import android.util.Xml
import com.dogear.reader.format.api.content.TocEntry
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream

/**
 * EPUB2 NCX table-of-contents parser. Produces a flat list of entries (nesting is flattened for
 * v1; a hierarchical NCX is uncommon and navigation works the same). Secure pull parsing with
 * DOCTYPE processing disabled.
 */
internal object NcxParser {

    fun parse(bytes: ByteArray, ncxRoot: String): List<TocEntry> = runCatching {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            runCatching { setFeature("http://xmlpull.org/v1/doc/features.html#process-docdecl", false) }
            setInput(ByteArrayInputStream(bytes), null)
        }
        val entries = mutableListOf<TocEntry>()
        var label: String? = null
        var src: String? = null
        var inNavLabelText = false

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "navPoint" -> { label = null; src = null }
                    "text" -> inNavLabelText = true
                    "content" -> src = parser.getAttributeValue(null, "src")
                }
                XmlPullParser.TEXT -> if (inNavLabelText && label == null) label = parser.text?.trim()
                XmlPullParser.END_TAG -> when (parser.name) {
                    "text" -> inNavLabelText = false
                    "navPoint" -> {
                        val title = label
                        val href = src
                        if (!title.isNullOrBlank() && !href.isNullOrBlank()) {
                            entries += TocEntry(title, resolveHref(href, ncxRoot))
                        }
                    }
                }
            }
            event = parser.next()
        }
        entries
    }.getOrDefault(emptyList())
}
