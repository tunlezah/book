package com.dogear.reader.format.api.content

import android.graphics.Bitmap
import android.util.Size

/**
 * Opened, readable content for a book. One of three render paths (Architecture §4): reflowable
 * (EPUB/HTML/TXT), fixed page (PDF), or image pager (CBZ). The reader's engines consume these;
 * each is responsible for lazy, bounded access (one spine item / one page at a time).
 */
sealed interface BookContent {

    /** Reflowable text: a spine of XHTML documents plus on-demand resources and a TOC. */
    interface Reflowable : BookContent {
        suspend fun spine(): List<SpineItem>
        suspend fun toc(): List<TocEntry>

        /** Sanitized, ready-to-render document for [spineIndex] (scripts stripped, URLs rewritten). */
        suspend fun document(spineIndex: Int): ReflowDocument

        /** A resource (image/css/font) referenced by a document, by its book-relative [path]. */
        suspend fun resource(path: String): Resource?
    }

    /** Fixed pages rendered to bitmaps (PDF). */
    interface FixedPage : BookContent {
        val pageCount: Int
        suspend fun renderPage(index: Int, target: Size): Bitmap?
    }

    /** A sequence of full-page images (CBZ). */
    interface ImagePager : BookContent {
        val pageCount: Int
        suspend fun page(index: Int, target: Size): Bitmap?
    }

    /** Releases any native/file handles. Always called when the reader closes. */
    fun close() {}
}

data class SpineItem(val index: Int, val href: String, val mediaType: String?)

data class TocEntry(val title: String, val href: String, val children: List<TocEntry> = emptyList())

/** Sanitized HTML for one spine document plus the base path used to resolve its resources. */
data class ReflowDocument(val html: String, val basePath: String)

class Resource(val bytes: ByteArray, val mimeType: String)
