package com.dogear.reader.core.model

/**
 * Core domain model for a book in the library. This is a pure-Kotlin type with no Android
 * or persistence dependencies so it can be shared across every module.
 */
data class Book(
    val id: Long = 0,
    val contentHash: String,
    val title: String,
    val subtitle: String? = null,
    val author: String,
    val authors: List<String> = emptyList(),
    val publisher: String? = null,
    val publishedDate: String? = null,
    val description: String? = null,
    val isbn: String? = null,
    val language: String? = null,
    val series: String? = null,
    val seriesIndex: Double? = null,
    val categories: List<String> = emptyList(),
    val format: BookFormat,
    val fileSizeBytes: Long = 0,
    val coverPath: String? = null,
    val coverIsGenerated: Boolean = false,
    val readingState: ReadingState = ReadingState.UNREAD,
    val addedAt: Long = 0,
    val lastOpenedAt: Long? = null,
    val metadataLocked: Boolean = false,
)

/**
 * Lightweight projection used by the shelf. Loading only the columns the grid/list needs
 * keeps paging cheap and memory low (see performance strategy).
 */
data class BookShelfItem(
    val id: Long,
    val title: String,
    val author: String,
    val coverPath: String?,
    val coverIsGenerated: Boolean,
    val format: BookFormat,
    val readingState: ReadingState,
    val progress: Float,
)
