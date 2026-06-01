package com.dogear.reader.format.text

import com.dogear.reader.format.api.FileRef
import com.dogear.reader.format.api.content.BookContent
import com.dogear.reader.format.api.content.ReflowDocument
import com.dogear.reader.format.api.content.Resource
import com.dogear.reader.format.api.content.SpineItem
import com.dogear.reader.format.api.content.TocEntry

/**
 * Plain text as a single reflowable document. We generate the HTML ourselves, so there is no
 * untrusted markup or script to sanitize. Paragraphs are split on blank lines; the reader's
 * injected CSS handles typography and pagination.
 */
internal class TxtContent(private val ref: FileRef) : BookContent.Reflowable {

    override suspend fun spine(): List<SpineItem> = listOf(SpineItem(0, "text", "text/plain"))

    override suspend fun toc(): List<TocEntry> = emptyList()

    override suspend fun resource(path: String): Resource? = null

    override suspend fun document(spineIndex: Int): ReflowDocument {
        val text = readText()
        val body = buildString {
            append("<html><head><meta charset=\"utf-8\"/></head><body>")
            text.split(Regex("\\n\\s*\\n")).forEach { paragraph ->
                val trimmed = paragraph.trim()
                if (trimmed.isNotEmpty()) {
                    append("<p>")
                    append(escapeHtml(trimmed).replace("\n", "<br/>"))
                    append("</p>")
                }
            }
            append("</body></html>")
        }
        return ReflowDocument(body, "")
    }

    private fun readText(): String = runCatching {
        ref.openInputStream().use { input ->
            val bytes = input.readBytes()
            decode(bytes)
        }
    }.getOrDefault("")

    /** BOM-aware decode; defaults to UTF-8 (lossless for ASCII/UTF-8 content). */
    private fun decode(bytes: ByteArray): String = when {
        bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte() ->
            String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() ->
            String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
        bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() ->
            String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
        else -> String(bytes, Charsets.UTF_8)
    }

    private fun escapeHtml(s: String): String = buildString(s.length) {
        for (c in s) when (c) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            else -> append(c)
        }
    }
}
