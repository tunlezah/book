package com.dogear.reader.feature.reader.data

import com.dogear.reader.core.common.dispatchers.IoDispatcher
import com.dogear.reader.core.database.dao.AnnotationDao
import com.dogear.reader.core.database.mapper.toDomain
import com.dogear.reader.core.database.mapper.toEntity
import com.dogear.reader.core.model.Bookmark
import com.dogear.reader.core.model.Highlight
import com.dogear.reader.core.model.Note
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.roundToInt

/** Bookmarks, highlights, and notes for a book. Annotations are anchored by locator so they
 * survive re-imports where possible (Architecture §5). */
class AnnotationRepository @Inject constructor(
    private val dao: AnnotationDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    fun bookmarks(bookId: Long): Flow<List<Bookmark>> =
        dao.observeBookmarks(bookId).map { list -> list.map { it.toDomain() } }

    fun highlights(bookId: Long): Flow<List<Highlight>> =
        dao.observeHighlights(bookId).map { list -> list.map { it.toDomain() } }

    fun notes(bookId: Long): Flow<List<Note>> =
        dao.observeNotes(bookId).map { list -> list.map { it.toDomain() } }

    suspend fun addBookmark(bookmark: Bookmark): Long =
        withContext(io) { dao.insertBookmark(bookmark.toEntity()) }

    suspend fun deleteBookmark(id: Long) = withContext(io) { dao.deleteBookmark(id) }

    suspend fun addHighlight(highlight: Highlight): Long =
        withContext(io) { dao.insertHighlight(highlight.toEntity()) }

    suspend fun deleteHighlight(id: Long) = withContext(io) { dao.deleteHighlight(id) }

    suspend fun addNote(note: Note): Long = withContext(io) { dao.insertNote(note.toEntity()) }

    suspend fun updateNote(note: Note) = withContext(io) { dao.updateNote(note.toEntity()) }

    suspend fun deleteNote(id: Long) = withContext(io) { dao.deleteNote(id) }

    /** Exports a book's highlights and notes as Markdown for sharing. */
    suspend fun exportMarkdown(bookId: Long, title: String): String = withContext(io) {
        val notes = dao.getNotes(bookId).map { it.toDomain() }
        buildString {
            appendLine("# Notes — $title")
            appendLine()
            if (notes.isEmpty()) {
                appendLine("_No notes yet._")
            } else {
                notes.sortedBy { it.locator?.progression ?: 0f }.forEach { note ->
                    val pct = ((note.locator?.progression ?: 0f) * 100).roundToInt()
                    appendLine("- **$pct%** — ${note.body}")
                }
            }
        }
    }
}
