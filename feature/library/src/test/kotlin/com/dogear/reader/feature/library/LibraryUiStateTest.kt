package com.dogear.reader.feature.library

import com.dogear.reader.core.model.BookFormat
import com.dogear.reader.core.model.LibraryFilter
import com.dogear.reader.core.model.ReadingState
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LibraryUiStateTest {

    @Test
    fun isEmpty_reflectsBookCount() {
        assertThat(LibraryUiState(bookCount = 0).isEmpty).isTrue()
        assertThat(LibraryUiState(bookCount = 5).isEmpty).isFalse()
    }

    @Test
    fun hasActiveFilter_ignoresSearchQueryButReactsToFacets() {
        // A search term alone is not considered an "active filter" chip.
        val searchOnly = LibraryUiState(filter = LibraryFilter(query = "dune"))
        assertThat(searchOnly.hasActiveFilter).isFalse()

        val faceted = LibraryUiState(
            filter = LibraryFilter(readingState = ReadingState.READING, format = BookFormat.EPUB),
        )
        assertThat(faceted.hasActiveFilter).isTrue()
    }
}
