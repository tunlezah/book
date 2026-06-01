package com.dogear.reader.core.model

/**
 * Metadata extracted from a book file by a format handler. All fields are optional because
 * real-world files are inconsistent; the importer fills gaps (e.g. title from filename).
 */
data class BookMetadata(
    val title: String? = null,
    val subtitle: String? = null,
    val authors: List<String> = emptyList(),
    val publisher: String? = null,
    val publishedDate: String? = null,
    val description: String? = null,
    val isbn: String? = null,
    val language: String? = null,
    val series: String? = null,
    val seriesIndex: Double? = null,
    val categories: List<String> = emptyList(),
) {
    companion object {
        val Empty = BookMetadata()
    }
}
