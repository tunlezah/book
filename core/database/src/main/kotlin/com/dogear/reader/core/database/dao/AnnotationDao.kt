package com.dogear.reader.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dogear.reader.core.database.entity.BookmarkEntity
import com.dogear.reader.core.database.entity.HighlightEntity
import com.dogear.reader.core.database.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnnotationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Query("SELECT * FROM bookmarks WHERE book_id = :bookId ORDER BY progression")
    fun observeBookmarks(bookId: Long): Flow<List<BookmarkEntity>>

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: HighlightEntity): Long

    @Query("SELECT * FROM highlights WHERE book_id = :bookId ORDER BY progression")
    fun observeHighlights(bookId: Long): Flow<List<HighlightEntity>>

    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun deleteHighlight(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("SELECT * FROM notes WHERE book_id = :bookId ORDER BY created_at DESC")
    fun observeNotes(bookId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE book_id = :bookId ORDER BY created_at DESC")
    suspend fun getNotes(bookId: Long): List<NoteEntity>

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: Long)
}
