package com.dogear.reader.format.fb2

import com.dogear.reader.core.model.BookMetadata
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import java.util.Base64
import kotlin.math.min

/**
 * FB2 (FictionBook) parser. FB2 is a single XML document; we parse it with jsoup's XML parser,
 * read `title-info` metadata and the embedded base64 cover, and convert the `<body>` sections to
 * HTML for the reflow engine. Inline images reference embedded `<binary>` elements by id.
 */
internal object Fb2Parser {

    fun parse(bytes: ByteArray): Document = Jsoup.parse(decode(bytes), "", Parser.xmlParser())

    fun metadata(doc: Document): BookMetadata {
        val ti = doc.selectFirst("title-info")
        val authors = ti?.select("author")?.mapNotNull { a ->
            val name = listOf(
                a.selectFirst("first-name")?.text().orEmpty(),
                a.selectFirst("last-name")?.text().orEmpty(),
            ).filter { it.isNotBlank() }.joinToString(" ")
            name.ifBlank { a.selectFirst("nickname")?.text().orEmpty() }.takeIf { it.isNotBlank() }
        }.orEmpty()
        return BookMetadata(
            title = ti?.selectFirst("book-title")?.text()?.trim(),
            authors = authors,
            language = ti?.selectFirst("lang")?.text()?.trim(),
            description = ti?.selectFirst("annotation")?.text()?.trim(),
            categories = ti?.select("genre")?.map { it.text().trim() }?.filter { it.isNotBlank() }.orEmpty(),
            series = ti?.selectFirst("sequence")?.attr("name")?.takeIf { it.isNotBlank() },
        )
    }

    fun coverId(doc: Document): String? =
        doc.selectFirst("coverpage image")?.let { hrefOf(it) }?.removePrefix("#")?.takeIf { it.isNotBlank() }

    fun binary(doc: Document, id: String): Pair<ByteArray, String>? {
        val node = doc.select("binary").firstOrNull { it.attr("id") == id } ?: return null
        val data = runCatching { Base64.getMimeDecoder().decode(node.text().trim()) }.getOrNull() ?: return null
        val mime = node.attr("content-type").ifBlank { "image/jpeg" }
        return data to mime
    }

    fun bodyHtml(doc: Document): String = buildString {
        append("<html><head><meta charset=\"utf-8\"/></head><body>")
        doc.select("body").forEach { append(convert(it)) }
        append("</body></html>")
    }

    private fun convert(body: org.jsoup.nodes.Element): String {
        val clone = body.clone()
        clone.select("*").forEach { el ->
            when (el.tagName().lowercase()) {
                "section", "poem", "stanza", "epigraph", "annotation" -> el.tagName("div")
                "title" -> el.tagName("h2")
                "subtitle" -> el.tagName("h3")
                "emphasis" -> el.tagName("em")
                "strikethrough" -> el.tagName("s")
                "empty-line" -> el.tagName("br")
                "v" -> el.tagName("p")
                "cite" -> el.tagName("blockquote")
                "image" -> {
                    val href = hrefOf(el).removePrefix("#")
                    el.tagName("img")
                    el.clearAttributes()
                    el.attr("src", href)
                }
                "p", "strong", "sub", "sup", "code", "a" -> Unit
                else -> el.tagName("div")
            }
        }
        return clone.html()
    }

    private fun hrefOf(el: org.jsoup.nodes.Element): String =
        el.attr("l:href").ifBlank { el.attr("xlink:href") }.ifBlank { el.attr("href") }

    /** FB2 declares its charset in the XML prolog; default to UTF-8, honor windows-1251/UTF-16. */
    fun decode(bytes: ByteArray): String {
        val head = String(bytes, 0, min(200, bytes.size), Charsets.ISO_8859_1).lowercase()
        val charset = when {
            head.contains("windows-1251") -> runCatching { charset("windows-1251") }.getOrDefault(Charsets.UTF_8)
            head.contains("utf-16") -> Charsets.UTF_16
            else -> Charsets.UTF_8
        }
        return String(bytes, charset)
    }
}
