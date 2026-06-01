package com.dogear.reader.core.model

/**
 * Supported book formats and the render path each uses. v1 ships Tier-1 formats; the rest
 * are declared so metadata, sorting, and the format filter are ready when their handlers land
 * (see the Format Support Report).
 */
enum class BookFormat(
    val displayName: String,
    val extensions: Set<String>,
    val renderPath: RenderPath,
    val tier: Int,
) {
    EPUB("EPUB", setOf("epub"), RenderPath.REFLOW, tier = 1),
    EPUB3("EPUB3", setOf("epub"), RenderPath.REFLOW, tier = 1),
    PDF("PDF", setOf("pdf"), RenderPath.FIXED_PAGE, tier = 1),
    TXT("Text", setOf("txt"), RenderPath.REFLOW, tier = 1),
    CBZ("Comic (CBZ)", setOf("cbz"), RenderPath.IMAGE_PAGER, tier = 1),

    FB2("FictionBook", setOf("fb2"), RenderPath.REFLOW, tier = 2),
    HTML("HTML", setOf("html", "htm"), RenderPath.REFLOW, tier = 2),
    MOBI("MOBI", setOf("mobi"), RenderPath.REFLOW, tier = 2),
    AZW3("AZW3", setOf("azw3"), RenderPath.REFLOW, tier = 2),

    DOCX("Word (DOCX)", setOf("docx"), RenderPath.REFLOW, tier = 3),
    RTF("RTF", setOf("rtf"), RenderPath.REFLOW, tier = 3),
    CBR("Comic (CBR)", setOf("cbr"), RenderPath.IMAGE_PAGER, tier = 3),

    UNKNOWN("Unknown", emptySet(), RenderPath.REFLOW, tier = 99);

    companion object {
        fun fromExtension(extension: String?): BookFormat {
            if (extension.isNullOrBlank()) return UNKNOWN
            val ext = extension.lowercase().removePrefix(".")
            return entries.firstOrNull { ext in it.extensions } ?: UNKNOWN
        }
    }
}

enum class RenderPath { REFLOW, FIXED_PAGE, IMAGE_PAGER }
