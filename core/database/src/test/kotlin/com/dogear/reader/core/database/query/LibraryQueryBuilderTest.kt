package com.dogear.reader.core.database.query

import com.dogear.reader.core.model.BookFormat
import com.dogear.reader.core.model.LibraryFilter
import com.dogear.reader.core.model.LibraryQuery
import com.dogear.reader.core.model.ReadingState
import com.dogear.reader.core.model.SortOption
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LibraryQueryBuilderTest {

    @Test
    fun noFilter_hasNoWhereClause_andStableOrder() {
        val result = LibraryQueryBuilder.build(LibraryQuery(sort = SortOption.RECENTLY_READ))
        assertThat(result.sql).doesNotContain("WHERE")
        assertThat(result.sql).contains("ORDER BY")
        assertThat(result.sql).endsWith("b.id ASC")
        assertThat(result.args).isEmpty()
    }

    @Test
    fun filters_areBoundAsArgumentsNotConcatenated() {
        val result = LibraryQueryBuilder.build(
            LibraryQuery(
                sort = SortOption.TITLE,
                ascending = true,
                filter = LibraryFilter(
                    readingState = ReadingState.READING,
                    format = BookFormat.EPUB,
                    author = "Le Guin",
                ),
            ),
        )
        assertThat(result.sql).contains("b.reading_state = ?")
        assertThat(result.sql).contains("b.format = ?")
        assertThat(result.sql).contains("b.author = ?")
        assertThat(result.sql).contains("b.title COLLATE NOCASE ASC")
        assertThat(result.args).containsExactly("READING", "EPUB", "Le Guin").inOrder()
    }

    @Test
    fun search_buildsFtsPrefixMatchAndTagLike() {
        val result = LibraryQueryBuilder.build(
            LibraryQuery(filter = LibraryFilter(query = "hob bit")),
        )
        assertThat(result.sql).contains("books_fts MATCH ?")
        assertThat(result.sql).contains("t.name LIKE ?")
        assertThat(result.args).containsExactly("hob* bit*", "%hob bit%").inOrder()
    }

    @Test
    fun blankSearch_isIgnored() {
        val result = LibraryQueryBuilder.build(LibraryQuery(filter = LibraryFilter(query = "   ")))
        assertThat(result.sql).doesNotContain("MATCH")
        assertThat(result.args).isEmpty()
    }

    @Test
    fun progressSort_ordersByCoalescedProgression() {
        val result = LibraryQueryBuilder.build(LibraryQuery(sort = SortOption.PROGRESS))
        assertThat(result.sql).contains("COALESCE(rp.progression, 0) DESC")
    }
}
