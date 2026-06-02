package com.dogear.reader.format.epub

import android.net.Uri
import com.dogear.reader.format.api.content.BookContent
import com.dogear.reader.format.api.content.ReflowDocument
import com.dogear.reader.format.api.content.Resource
import com.dogear.reader.format.api.content.SpineItem
import com.dogear.reader.format.api.content.TocEntry
import com.dogear.reader.format.api.io.SafeZip
import org.jsoup.Jsoup
import java.io.File

/**
 * Reflowable EPUB content. Reads spine documents and resources lazily through [SafeZip] (one at
 * a time — never the whole book), sanitizes/charset-decodes each document, and exposes a TOC from
 * the EPUB3 nav doc or the EPUB2 NCX.
 *
 * Resource resolution is deliberately forgiving (Hardening research §9): EPUB hrefs are matched
 * against zip entries by exact name, then percent-decoded, backslash-normalized, and
 * case-insensitive — so books authored with `%20`, `Image.JPG` vs `image.jpg`, or `\` separators
 * still load their images/CSS/fonts instead of rendering broken.
 */
internal class EpubContent(private val file: File) : BookContent.Reflowable {

    private val entryNames: List<String> by lazy {
        runCatching { SafeZip.entryNames(file) }.getOrDefault(emptyList())
    }

    /** normalized key (decoded, lowercased, forward slashes) → actual zip entry name. */
    private val entryIndex: Map<String, String> by lazy {
        entryNames.associateBy { normalizeKey(it) }
    }

    /** OPF path: container.xml → first rootfile, else the first `*.opf` anywhere in the zip. */
    private val opfPath: String? by lazy {
        val fromContainer = readEntry("META-INF/container.xml")
            ?.let { OpfParser.parseContainer(it) }
        fromContainer ?: entryNames.firstOrNull { it.endsWith(".opf", ignoreCase = true) }
    }

    private val pkg: OpfParser.Package by lazy {
        val path = opfPath ?: return@lazy OpfParser.Package(emptyMap(), emptyList(), null, null)
        readEntry(path)?.let { OpfParser.parsePackage(it) }
            ?: OpfParser.Package(emptyMap(), emptyList(), null, null)
    }

    private val spineItems: List<SpineItem> by lazy {
        val base = opfPath ?: return@lazy emptyList()
        pkg.spineHrefs.mapIndexed { index, href ->
            val rootPath = resolveRelative(base, href)
            val mediaType = pkg.manifest.values.firstOrNull { it.href == href }?.mediaType
            SpineItem(index, rootPath, mediaType)
        }
    }

    override suspend fun spine(): List<SpineItem> = spineItems

    override suspend fun document(spineIndex: Int): ReflowDocument {
        val item = spineItems.getOrNull(spineIndex) ?: return ReflowDocument(EMPTY_DOC, "")
        val bytes = readEntry(item.href) ?: return ReflowDocument(EMPTY_DOC, "")
        val html = HtmlSanitizer.sanitize(bytes) // charset auto-detected, lenient parse
        val basePath = item.href.substringBeforeLast('/', "")
        return ReflowDocument(html, basePath)
    }

    override suspend fun resource(path: String): Resource? {
        val bytes = readEntry(path) ?: return null
        return Resource(bytes, resourceMime(path))
    }

    override suspend fun toc(): List<TocEntry> {
        val base = opfPath ?: return emptyList()
        pkg.navHref?.let { nav ->
            val navRoot = resolveRelative(base, nav)
            readEntry(navRoot)?.let { return parseNav(it.toString(Charsets.UTF_8), navRoot) }
        }
        pkg.ncxHref?.let { ncx ->
            val ncxRoot = resolveRelative(base, ncx)
            readEntry(ncxRoot)?.let { return NcxParser.parse(it, ncxRoot) }
        }
        return emptyList()
    }

    /** Reads a zip entry by book-root path, tolerating encoding/case/separator mismatches. */
    private fun readEntry(path: String): ByteArray? {
        val actual = resolveEntry(path) ?: return null
        return SafeZip.readEntry(file, actual)
    }

    private fun resolveEntry(path: String): String? {
        if (path in entryNames) return path
        return entryIndex[normalizeKey(path)]
    }

    private fun normalizeKey(raw: String): String =
        runCatching { Uri.decode(raw) }.getOrDefault(raw)
            .replace('\\', '/')
            .removePrefix("/")
            .lowercase()

    private fun parseNav(html: String, navRoot: String): List<TocEntry> = runCatching {
        val doc = Jsoup.parse(html)
        val navs = doc.select("nav")
        val toc = navs.firstOrNull { it.attr("epub:type").contains("toc") } ?: navs.firstOrNull()
        val rootList = toc?.selectFirst("ol") ?: return emptyList()
        parseNavList(rootList, navRoot)
    }.getOrDefault(emptyList())

    private fun parseNavList(ol: org.jsoup.nodes.Element, navRoot: String): List<TocEntry> =
        ol.children().filter { it.tagName() == "li" }.mapNotNull { li ->
            val anchor = li.selectFirst("a") ?: return@mapNotNull null
            val title = anchor.text().trim().ifEmpty { return@mapNotNull null }
            val href = resolveHref(anchor.attr("href"), navRoot)
            val childList = li.selectFirst("ol")
            val children = if (childList != null) parseNavList(childList, navRoot) else emptyList()
            TocEntry(title, href, children)
        }

    private fun resourceMime(path: String): String = when (path.substringAfterLast('.', "").lowercase()) {
        "xhtml", "html", "htm" -> "text/html"
        "css" -> "text/css"
        "js" -> "text/javascript"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "svg" -> "image/svg+xml"
        "ttf" -> "font/ttf"
        "otf" -> "font/otf"
        "woff" -> "font/woff"
        "woff2" -> "font/woff2"
        else -> "application/octet-stream"
    }

    private companion object {
        const val EMPTY_DOC = "<html><body></body></html>"
    }
}

/** Resolves a TOC/anchor href (which may carry a #fragment) relative to its document root path. */
internal fun resolveHref(href: String, docRoot: String): String {
    val hash = href.indexOf('#')
    val path = if (hash >= 0) href.substring(0, hash) else href
    val fragment = if (hash >= 0) href.substring(hash) else ""
    val resolvedPath = if (path.isEmpty()) docRoot else resolveRelative(docRoot, path)
    return resolvedPath + fragment
}
