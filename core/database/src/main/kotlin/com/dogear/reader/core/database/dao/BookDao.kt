package com.dogear.reader.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Upsert
import androidx.sqlite.db.SupportSQLiteQuery
import com.dogear.reader.core.database.entity.BookCollectionCrossRef
import com.dogear.reader.core.database.entity.BookEntity
import com.dogear.reader.core.database.entity.BookFileEntity
import com.dogear.reader.core.database.entity.BookTagCrossRef
import com.dogear.reader.core.database.entity.ReadingProgressEntity
import com.dogear.reader.core.database.model.ShelfRow
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    /**
     * Paged shelf query. The [query] is produced by
     * [com.dogear.reader.core.database.query.LibraryQueryBuilder]; `observedEntities` makes the
     * pager invalidate when any table affecting the result changes.
     */
    @RawQuery(
        observedEntities = [
            BookEntity::class,
            ReadingProgressEntity::class,
            BookCollectionCrossRef::class,
            BookTagCrossRef::class,
        ],
    )
    fun shelf(query: SupportSQLiteQuery): PagingSource<Int, ShelfRow>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(book: BookEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(books: List<BookEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFile(file: BookFileEntity): Long

    @Upsert
    suspend fun upsert(book: BookEntity)

    @Query("SELECT * FROM books WHERE id = :id")
    fun observeBook(id: Long): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBook(id: Long): BookEntity?

    @Query("SELECT * FROM book_files WHERE book_id = :bookId LIMIT 1")
    suspend fun getPrimaryFile(bookId: Long): BookFileEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM books WHERE content_hash = :hash)")
    suspend fun existsByHash(hash: String): Boolean

    @Query("SELECT COUNT(*) FROM books")
    fun observeCount(): Flow<Int>

    @Query("SELECT DISTINCT author FROM books ORDER BY author COLLATE NOCASE")
    fun observeAuthors(): Flow<List<String>>

    @Query("SELECT DISTINCT format FROM books")
    fun observeFormats(): Flow<List<String>>

    @Query("UPDATE books SET reading_state = :state WHERE id = :id")
    suspend fun updateReadingState(id: Long, state: String)

    @Query("UPDATE books SET last_opened_at = :time WHERE id = :id")
    suspend fun updateLastOpened(id: Long, time: Long)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBook(id: Long)
}
