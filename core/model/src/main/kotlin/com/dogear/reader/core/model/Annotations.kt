package com.dogear.reader.core.model

/**
 * A format-agnostic reading position. `progression` (0..1 within the whole book) is always
 * set as a robust fallback; the other fields anchor precisely and survive re-imports where
 * possible (see Architecture: Locator system).
 */
data class Locator(
    val spineIndex: Int? = null,
    val selector: String? = null,
    val charOffset: Int? = null,
    val textSnippet: String? = null,
    val pageIndex: Int? = null,
    val progression: Float = 0f,
)

data class ReadingProgress(
    val bookId: Long,
    val locator: Locator,
    val chapterTitle: String? = null,
    val updatedAt: Long = 0,
)

data class Bookmark(
    val id: Long = 0,
    val bookId: Long,
    val name: String? = null,
    val locator: Locator,
    val excerpt: String? = null,
    val createdAt: Long = 0,
)

data class Highlight(
    val id: Long = 0,
    val bookId: Long,
    val color: Int,
    val selectedText: String,
    val start: Locator,
    val end: Locator,
    val createdAt: Long = 0,
)

data class Note(
    val id: Long = 0,
    val bookId: Long,
    val highlightId: Long? = null,
    val body: String,
    val locator: Locator? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
