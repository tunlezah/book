package com.dogear.reader.core.model

/** How the shelf is laid out. */
enum class ViewMode { GRID, LIST }

/** Sort options required by the library spec. */
enum class SortOption(val displayName: String) {
    RECENTLY_READ("Recently read"),
    RECENTLY_ADDED("Recently added"),
    TITLE("Title"),
    AUTHOR("Author"),
    FILE_SIZE("File size"),
    PROGRESS("Reading progress"),
}

/**
 * A combinable set of library filters. A null/empty field means "no constraint on this
 * dimension". The repository translates this into a parameterized SQL query.
 */
data class LibraryFilter(
    val author: String? = null,
    val format: BookFormat? = null,
    val readingState: ReadingState? = null,
    val collectionId: Long? = null,
    val tagId: Long? = null,
    val query: String? = null,
) {
    val isEmpty: Boolean
        get() = author == null && format == null && readingState == null &&
            collectionId == null && tagId == null && query.isNullOrBlank()

    companion object {
        val None = LibraryFilter()
    }
}

/** The full description of what the shelf should show. */
data class LibraryQuery(
    val sort: SortOption = SortOption.RECENTLY_READ,
    val ascending: Boolean = false,
    val filter: LibraryFilter = LibraryFilter.None,
)
