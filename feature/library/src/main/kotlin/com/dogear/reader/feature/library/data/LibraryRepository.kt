package com.dogear.reader.feature.library.data

import androidx.paging.PagingData
import com.dogear.reader.core.model.Book
import com.dogear.reader.core.model.BookFormat
import com.dogear.reader.core.model.BookShelfItem
import com.dogear.reader.core.model.Collection
import com.dogear.reader.core.model.LibraryQuery
import com.dogear.reader.core.model.ReadingState
import com.dogear.reader.core.model.Tag
import kotlinx.coroutines.flow.Flow

/**
 * The single source of truth for the library feature. Hides Room/paging details behind domain
 * types so ViewModels and UI never touch DAOs or entities (Repository pattern, Architecture §3).
 */
interface LibraryRepository {

    fun pagedShelf(query: LibraryQuery): Flow<PagingData<BookShelfItem>>

    fun observeBook(id: Long): Flow<Book?>

    fun observeBookCount(): Flow<Int>

    fun observeAuthors(): Flow<List<String>>

    fun observeFormats(): Flow<List<BookFormat>>

    fun observeCollections(): Flow<List<Collection>>

    fun observeTags(): Flow<List<Tag>>

    suspend fun setReadingState(bookId: Long, state: ReadingState)

    suspend fun updateBook(book: Book)

    suspend fun markOpened(bookId: Long)

    suspend fun deleteBook(bookId: Long)

    /**
     * Seeds synthetic books so the shelf can be exercised before the import/format modules
     * exist (debug + empty-state affordance, and the basis for performance seeding).
     */
    suspend fun seedSampleBooks(count: Int)
}
