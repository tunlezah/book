package com.dogear.reader.feature.library

import com.dogear.reader.core.model.BookFormat
import com.dogear.reader.core.model.Collection
import com.dogear.reader.core.model.LibraryFilter
import com.dogear.reader.core.model.SortOption
import com.dogear.reader.core.model.Tag
import com.dogear.reader.core.model.ViewMode

/** Filter dimensions the shelf can offer, derived from what is actually in the library. */
data class FilterOptions(
    val authors: List<String> = emptyList(),
    val formats: List<BookFormat> = emptyList(),
    val collections: List<Collection> = emptyList(),
    val tags: List<Tag> = emptyList(),
)

/** Everything the shelf chrome (top bar, sort/filter, empty state) needs, minus the paged list. */
data class LibraryUiState(
    val viewMode: ViewMode = ViewMode.GRID,
    val sort: SortOption = SortOption.RECENTLY_READ,
    val sortAscending: Boolean = false,
    val filter: LibraryFilter = LibraryFilter.None,
    val searchQuery: String = "",
    val bookCount: Int = 0,
    val options: FilterOptions = FilterOptions(),
) {
    val isEmpty: Boolean get() = bookCount == 0
    val hasActiveFilter: Boolean get() = !filter.copy(query = null).isEmpty
}
