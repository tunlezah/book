package com.dogear.reader.format.api

import com.dogear.reader.core.model.BookFormat
import com.dogear.reader.core.model.BookMetadata
import com.dogear.reader.format.api.content.BookContent

/**
 * The format extension point (Architecture §4). Each handler probes, extracts metadata and a
 * cover, and opens readable [BookContent] for the reader.
 *
 * Handlers must be defensive: malformed input is rejected (return null / false), never crashes
 * (see Security Review). All methods run off the main thread.
 */
interface BookFormatHandler {

    /** Formats this handler can produce. Used for the format filter and explicit lookups. */
    val supportedFormats: Set<BookFormat>

    /**
     * Higher runs first during detection, so signature-based handlers (PDF/EPUB/CBZ) win over
     * the catch-all text handler.
     */
    val priority: Int

    /** Cheap structural/magic-byte check that this handler can read [ref]. */
    suspend fun probe(ref: FileRef): ProbeResult

    suspend fun extractMetadata(ref: FileRef): BookMetadata

    /** The embedded cover if present, else null (the importer then generates one). */
    suspend fun extractCover(ref: FileRef): RawImage?

    /** Opens readable content for the reader. The caller must [BookContent.close] it when done. */
    suspend fun openContent(ref: FileRef): BookContent
}

/** The outcome of a probe: whether this handler matches, and the concrete format if so. */
data class ProbeResult(val matches: Boolean, val format: BookFormat = BookFormat.UNKNOWN) {
    companion object {
        val NoMatch = ProbeResult(false)
        fun of(format: BookFormat) = ProbeResult(true, format)
    }
}
