package com.dogear.reader.feature.library.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.dogear.reader.core.common.dispatchers.IoDispatcher
import com.dogear.reader.core.database.ShelfPagingSourceFactory
import com.dogear.reader.core.database.dao.BookDao
import com.dogear.reader.core.database.dao.OrganizationDao
import com.dogear.reader.core.database.mapper.toDomain
import com.dogear.reader.core.database.mapper.toEntity
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

    override suspend fun updateBook(book: Book) =
        withContext(ioDispatcher) { bookDao.upsert(book.toEntity()) }

    override suspend fun markOpened(bookId: Long) =
        withContext(ioDispatcher) { bookDao.updateLastOpened(bookId, System.currentTimeMillis()) }

    override suspend fun deleteBook(bookId: Long) =
        withContext(ioDispatcher) { bookDao.deleteBook(bookId) }

    private companion object {
        const val PAGE_SIZE = 40
    }
}
