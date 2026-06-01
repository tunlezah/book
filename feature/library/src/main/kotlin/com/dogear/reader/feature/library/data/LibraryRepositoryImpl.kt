package com.dogear.reader.feature.library.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.dogear.reader.core.common.dispatchers.IoDispatcher
import com.dogear.reader.core.database.ShelfPagingSourceFactory
import com.dogear.reader.core.database.dao.BookDao
import com.dogear.reader.core.database.dao.OrganizationDao
import com.dogear.reader.core.database.entity.BookEntity
import com.dogear.reader.core.database.mapper.toDomain
import com.dogear.reader.core.model.Book
import com.dogear.reader.core.model.BookFormat
import com.dogear.reader.core.model.BookShelfItem
import com.dogear.reader.core.model.Collection
import com.dogear.reader.core.model.LibraryQuery
import com.dogear.reader.core.model.ReadingState
import com.dogear.reader.core.model.Tag
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.random.Random

class LibraryRepositoryImpl @Inject constructor(
    private val bookDao: BookDao,
    private val organizationDao: OrganizationDao,
    private val shelfPagingSourceFactory: ShelfPagingSourceFactory,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : LibraryRepository {

    override fun pagedShelf(query: LibraryQuery): Flow<PagingData<BookShelfItem>> =
        Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                prefetchDistance = PAGE_SIZE / 2,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = { shelfPagingSourceFactory.create(query) },
        ).flow.map { pagingData -> pagingData.map { it.toDomain() } }

    override fun observeBook(id: Long): Flow<Book?> =
        bookDao.observeBook(id).map { it?.toDomain() }

    override fun observeBookCount(): Flow<Int> = bookDao.observeCount()

    override fun observeAuthors(): Flow<List<String>> = bookDao.observeAuthors()

    override fun observeFormats(): Flow<List<BookFormat>> =
        bookDao.observeFormats().map { names ->
            names.mapNotNull { name -> BookFormat.entries.firstOrNull { it.name == name } }
        }

    override fun observeCollections(): Flow<List<Collection>> =
        organizationDao.observeCollections().map { list ->
            list.map { Collection(it.id, it.name, it.sortOrder, it.createdAt) }
        }

    override fun observeTags(): Flow<List<Tag>> =
        organizationDao.observeTags().map { list -> list.map { Tag(it.id, it.name) } }

    override suspend fun setReadingState(bookId: Long, state: ReadingState) =
        withContext(ioDispatcher) { bookDao.updateReadingState(bookId, state.name) }

    override suspend fun markOpened(bookId: Long) =
        withContext(ioDispatcher) { bookDao.updateLastOpened(bookId, System.currentTimeMillis()) }

    override suspend fun deleteBook(bookId: Long) =
        withContext(ioDispatcher) { bookDao.deleteBook(bookId) }

    override suspend fun seedSampleBooks(count: Int) = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val formats = listOf(BookFormat.EPUB, BookFormat.PDF, BookFormat.TXT, BookFormat.CBZ)
        val states = ReadingState.entries
        val books = (0 until count).map { i ->
            val title = "${SampleTitles.random(i)} ${i + 1}"
            BookEntity(
                contentHash = "sample-${now}-$i",
                title = title,
                author = SampleAuthors.random(i),
                format = formats[i % formats.size].name,
                fileSize = Random(i.toLong()).nextLong(80_000, 12_000_000),
                readingState = states[i % states.size].name,
                addedAt = now - i * 60_000L,
                lastOpenedAt = if (i % 3 == 0) now - i * 30_000L else null,
                coverIsGenerated = true,
            )
        }
        bookDao.insertAll(books)
        Unit
    }

    private companion object {
        const val PAGE_SIZE = 40
    }
}

private object SampleTitles {
    private val words = listOf(
        "The Quiet", "A Distant", "Pale", "The Last", "Northern", "Hollow", "Bright",
        "The Glass", "Salt", "The Long", "Ember", "The Folded", "Paper", "The Silent",
    )
    private val nouns = listOf(
        "Library", "Shore", "Garden", "Machine", "Lantern", "Atlas", "Harbor", "Field",
        "Country", "Archive", "Page", "Reader", "Tide", "Forest",
    )

    fun random(seed: Int): String =
        "${words[seed % words.size]} ${nouns[(seed / 2) % nouns.size]}"
}

private object SampleAuthors {
    private val first = listOf("Ada", "Marcus", "Lena", "Idris", "Nora", "Owen", "Priya", "Sam")
    private val last = listOf("Vance", "Okafor", "Holt", "Maren", "Castellano", "Wu", "Bergström")

    fun random(seed: Int): String =
        "${first[seed % first.size]} ${last[(seed / 3) % last.size]}"
}
