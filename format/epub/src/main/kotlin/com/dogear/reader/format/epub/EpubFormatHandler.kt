package com.dogear.reader.format.epub

import com.dogear.reader.core.model.BookFormat
import com.dogear.reader.core.model.BookMetadata
import com.dogear.reader.format.api.BookFormatHandler
import com.dogear.reader.format.api.FileRef
import com.dogear.reader.format.api.ProbeResult
import com.dogear.reader.format.api.RawImage
import com.dogear.reader.format.api.content.BookContent
import com.dogear.reader.format.api.io.SafeZip
import com.dogear.reader.format.api.materialize
import java.io.File
import java.net.URLDecoder
import javax.inject.Inject

/**
 * EPUB 2/3 handler: reads the OCF container → OPF for metadata and the cover image. Everything
 * is read through [SafeZip], so malformed/zip-bomb archives are bounded and rejected safely.
 */
class EpubFormatHandler @Inject constructor() : BookFormatHandler {

    override val supportedFormats: Set<BookFormat> = setOf(BookFormat.EPUB, BookFormat.EPUB3)
    override val priority: Int = 100

    override suspend fun probe(ref: FileRef): ProbeResult {
        val file = runCatching { ref.materialize() }.getOrNull() ?: return ProbeResult.NoMatch
        if (!SafeZip.isZip(file)) return ProbeResult.NoMatch
        val mimetype = SafeZip.readEntry(file, "mimetype")?.toString(Charsets.US_ASCII)?.trim()
        if (mimetype == EPUB_MIME) return ProbeResult.of(detectVersion(file))
        // Some EPUBs omit/misplace the mimetype entry; accept a valid container instead.
        val hasContainer = SafeZip.readEntry(file, CONTAINER_PATH) != null
        return if (hasContainer) ProbeResult.of(detectVersion(file)) else ProbeResult.NoMatch
    }

    override suspend fun extractMetadata(ref: FileRef): BookMetadata {
        val file = ref.materialize()
        val opf = readOpf(file) ?: return BookMetadata.Empty
        return opf.metadata
    }

    override suspend fun openContent(ref: FileRef): BookContent = EpubContent(ref.materialize())

    override suspend fun extractCover(ref: FileRef): RawImage? {
        val file = ref.materialize()
        val opfPath = opfPath(file) ?: return null
        val opfBytes = SafeZip.readEntry(file, opfPath) ?: return null
        val href = OpfParser.parseOpf(opfBytes).coverHref ?: return null
        val coverPath = resolveRelative(opfPath, URLDecoder.decode(href, "UTF-8"))
        val bytes = SafeZip.readEntry(file, coverPath) ?: return null
        return RawImage(bytes, mimeFromExtension(coverPath))
    }

    private fun readOpf(file: File): OpfParser.Opf? {
        val path = opfPath(file) ?: return null
        val bytes = SafeZip.readEntry(file, path) ?: return null
        return OpfParser.parseOpf(bytes)
    }

    private fun opfPath(file: File): String? {
        val container = SafeZip.readEntry(file, CONTAINER_PATH) ?: return null
        return OpfParser.parseContainer(container)
    }

    /** EPUB3 if any OPF declares version 3; cheap heuristic without full parse. */
    private fun detectVersion(file: File): BookFormat {
        val path = opfPath(file) ?: return BookFormat.EPUB
        val opf = SafeZip.readEntry(file, path)?.toString(Charsets.UTF_8) ?: return BookFormat.EPUB
        val versionMatch = Regex("""<package[^>]*version="([0-9.]+)"""").find(opf)
        return if (versionMatch?.groupValues?.getOrNull(1)?.startsWith("3") == true) {
            BookFormat.EPUB3
        } else {
            BookFormat.EPUB
        }
    }

    private companion object {
        const val EPUB_MIME = "application/epub+zip"
        const val CONTAINER_PATH = "META-INF/container.xml"
    }
}

/** Resolves [href] relative to the directory of [base], normalizing `.` and `..`. */
internal fun resolveRelative(base: String, href: String): String {
    if (href.startsWith("/")) return href.trimStart('/')
    val baseDir = base.substringBeforeLast('/', "")
    val combined = if (baseDir.isEmpty()) href else "$baseDir/$href"
    val stack = ArrayDeque<String>()
    for (part in combined.split('/')) {
        when (part) {
            "", "." -> {}
            ".." -> if (stack.isNotEmpty()) stack.removeLast()
            else -> stack.addLast(part)
        }
    }
    return stack.joinToString("/")
}

internal fun mimeFromExtension(path: String): String = when (path.substringAfterLast('.').lowercase()) {
    "jpg", "jpeg" -> "image/jpeg"
    "png" -> "image/png"
    "gif" -> "image/gif"
    "webp" -> "image/webp"
    "svg" -> "image/svg+xml"
    else -> "image/*"
}
