package com.dogear.reader.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.dogear.reader.core.database.entity.BookEntity
import com.dogear.reader.core.database.entity.BookmarkEntity
import com.dogear.reader.core.database.entity.NoteEntity
import com.dogear.reader.core.model.BookFormat
import com.dogear.reader.core.model.ReadingState
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AnnotationDaoTest {

    private lateinit var db: DogearDatabase
    private var bookId: Long = 0

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DogearDatabase::class.java,
        ).allowMainThreadQueries().build()
        bookId = db.bookDao().insert(
            BookEntity(
                contentHash = "h1",
                title = "Book",
                author = "Author",
                format = BookFormat.EPUB.name,
                readingState = ReadingState.UNREAD.name,
            ),
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun bookmarks_insertObserveDelete() = runTest {
        val dao = db.annotationDao()
        val id = dao.insertBookmark(BookmarkEntity(bookId = bookId, name = "Start", progression = 0.1f))
        assertThat(dao.observeBookmarks(bookId).first().map { it.name }).containsExactly("Start")
        dao.deleteBookmark(id)
        assertThat(dao.observeBookmarks(bookId).first()).isEmpty()
    }

    @Test
    fun notes_orderNewestFirst() = runTest {
        val dao = db.annotationDao()
        dao.insertNote(NoteEntity(bookId = bookId, body = "first", createdAt = 1))
        dao.insertNote(NoteEntity(bookId = bookId, body = "second", createdAt = 2))
        assertThat(dao.observeNotes(bookId).first().map { it.body })
            .containsExactly("second", "first").inOrder()
    }

    @Test
    fun deletingBook_cascadesAnnotations() = runTest {
        val dao = db.annotationDao()
        dao.insertNote(NoteEntity(bookId = bookId, body = "note"))
        db.bookDao().deleteBook(bookId)
        assertThat(dao.observeNotes(bookId).first()).isEmpty()
    }
}
