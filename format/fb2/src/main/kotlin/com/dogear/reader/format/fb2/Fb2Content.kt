package com.dogear.reader.format.fb2

import com.dogear.reader.format.api.content.BookContent
import com.dogear.reader.format.api.content.ReflowDocument
import com.dogear.reader.format.api.content.Resource
import com.dogear.reader.format.api.content.SpineItem
import com.dogear.reader.format.api.content.TocEntry

/** Reflowable FB2 content: the whole book as one HTML document; images come from `<binary>`. */
internal class Fb2Content(private val bytes: ByteArray) : BookContent.Reflowable {

    private val doc by lazy { Fb2Parser.parse(bytes) }

    override suspend fun spine(): List<SpineItem> = listOf(SpineItem(0, "fb2", "application/x-fictionbook+xml"))

    override suspend fun toc(): List<TocEntry> = emptyList()

    override suspend fun document(spineIndex: Int): ReflowDocument =
        ReflowDocument(Fb2Parser.bodyHtml(doc), "")

    override suspend fun resource(path: String): Resource? {
        val (data, mime) = Fb2Parser.binary(doc, path) ?: return null
        return Resource(data, mime)
    }
}
