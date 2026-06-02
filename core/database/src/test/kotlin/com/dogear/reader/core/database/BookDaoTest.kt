package com.dogear.reader.core.database

import androidx.paging.PagingSource
import androidx.room.Room
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import com.dogear.reader.core.database.dao.BookDao
import com.dogear.reader.core.database.entity.BookEntity
import com.dogear.reader.core.database.model.ShelfRow
import com.dogear.reader.core.database.query.LibraryQueryBuilder
import com.dogear.reader.core.model.BookFormat
import com.dogear.reader.core.model.LibraryFilter
import com.dogear.reader.core.model.LibraryQuery
import com.dogear.reader.core.model.ReadingState
import com.dogear.reader.core.model.SortOption
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BookDaoTest {

    private lateinit var db: DogearDatabase
    private lateinit var dao: BookDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DogearDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.bookDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun book(title: String, author: String, hash: String, added: Long) = BookEntity(
        contentHash = hash,
        title = title,
        author = author,
        format = BookFormat.EPUB.name,
        readingState = ReadingState.UNREAD.name,
        addedAt = added,
    )

    @Test
    fun insert_dedupesByContentHash() = runTest {
        val first = dao.insert(book("A", "Author", "hash-1", 1))
        val duplicate = dao.insert(book("A again", "Author", "hash-1", 2))
        assertThat(first).isGreaterThan(0L)
        assertThat(duplicate).isEqualTo(-1L) // IGNORE conflict
        assertThat(dao.existsByHash("hash-1")).isTrue()
    }

    @Test
    fun shelf_sortsByTitleAscending() = runTest {
        dao.insert(book("Zebra", "X", "h1", 1))
        dao.insert(book("apple", "Y", "h2", 2))
        dao.insert(book("Mango", "Z", "h3", 3))

        val rows = loadShelf(LibraryQuery(sort = SortOption.TITLE, ascending = true))

        assertThat(rows.map { it.title }).containsExactly("apple", "Mango", "Zebra").inOrder()
    }

    @Test
    fun shelf_filtersByReadingState() = runTest {
        val readingId = dao.insert(book("Reading one", "X", "h1", 1))
        dao.insert(book("Unread one", "Y", "h2", 2))
        dao.updateReadingState(readingId, ReadingState.READING.name)

        val rows = loadShelf(
            LibraryQuery(filter = LibraryFilter(readingState = ReadingState.READING)),
        )

        assertThat(rows.map { it.title }).containsExactly("Reading one")
    }

    private suspend fun loadShelf(query: LibraryQuery): List<ShelfRow> {
        val raw = LibraryQueryBuilder.build(query)
        val source = dao.shelf(SimpleSQLiteQuery(raw.sql, raw.args.toTypedArray()))
        val result = source.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 50,
                placeholdersEnabled = false,
            ),
        )
        return (result as PagingSource.LoadResult.Page).data
    }
}
