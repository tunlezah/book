package com.dogear.reader.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dogear.reader.core.database.entity.BookCollectionCrossRef
import com.dogear.reader.core.database.entity.BookEntity
import com.dogear.reader.core.database.entity.BookFileEntity
import com.dogear.reader.core.database.entity.BookTagCrossRef
import com.dogear.reader.core.database.entity.BookmarkEntity
import com.dogear.reader.core.database.entity.CollectionEntity
import com.dogear.reader.core.database.entity.HighlightEntity
import com.dogear.reader.core.database.entity.NoteEntity
import com.dogear.reader.core.database.entity.ReadingProgressEntity
import com.dogear.reader.core.database.entity.TagEntity

/** Bulk read/restore across all user tables for backup & restore (Database Schema §6). */
@Dao
interface BackupDao {

    @Query("SELECT * FROM books") suspend fun books(): List<BookEntity>
    @Query("SELECT * FROM book_files") suspend fun files(): List<BookFileEntity>
    @Query("SELECT * FROM reading_progress") suspend fun progress(): List<ReadingProgressEntity>
    @Query("SELECT * FROM bookmarks") suspend fun bookmarks(): List<BookmarkEntity>
    @Query("SELECT * FROM highlights") suspend fun highlights(): List<HighlightEntity>
    @Query("SELECT * FROM notes") suspend fun notes(): List<NoteEntity>
    @Query("SELECT * FROM collections") suspend fun collections(): List<CollectionEntity>
    @Query("SELECT * FROM tags") suspend fun tags(): List<TagEntity>
    @Query("SELECT * FROM book_collections") suspend fun bookCollections(): List<BookCollectionCrossRef>
    @Query("SELECT * FROM book_tags") suspend fun bookTags(): List<BookTagCrossRef>

    /** Clearing books cascades to all per-book children (FK ON DELETE CASCADE). */
    @Query("DELETE FROM books") suspend fun clearBooks()
    @Query("DELETE FROM collections") suspend fun clearCollections()
    @Query("DELETE FROM tags") suspend fun clearTags()

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreBooks(list: List<BookEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreFiles(list: List<BookFileEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreProgress(list: List<ReadingProgressEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreBookmarks(list: List<BookmarkEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreHighlights(list: List<HighlightEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreNotes(list: List<NoteEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreCollections(list: List<CollectionEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreTags(list: List<TagEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreBookCollections(list: List<BookCollectionCrossRef>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreBookTags(list: List<BookTagCrossRef>)
}
