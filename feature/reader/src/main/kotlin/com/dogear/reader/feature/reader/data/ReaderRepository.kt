package com.dogear.reader.feature.reader.data

import com.dogear.reader.core.common.dispatchers.IoDispatcher
import com.dogear.reader.core.database.dao.BookDao
import com.dogear.reader.core.database.dao.ReadingProgressDao
import com.dogear.reader.core.database.entity.ReadingProgressEntity
import com.dogear.reader.core.database.mapper.toDomain
import com.dogear.reader.core.model.Book
import com.dogear.reader.core.model.Locator
import com.dogear.reader.core.model.ReadingState
import com.dogear.reader.format.api.FileBackedRef
import com.dogear.reader.format.api.FormatRegistry
import com.dogear.reader.format.api.content.BookContent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * Loads a book's readable content and persists reading position. Position writes are atomic
 * single-row upserts (Architecture §5), so the last page survives crashes and force-kill.
 */
class ReaderRepository @Inject constructor(
    private val bookDao: BookDao,
    private val progressDao: ReadingProgressDao,
    private val formatRegistry: FormatRegistry,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    suspend fun open(bookId: Long): OpenedBook? = withContext(io) {
        val entity = bookDao.getBook(bookId) ?: return@withContext null
        val fileRow = bookDao.getPrimaryFile(bookId) ?: return@withContext null
        val file = File(fileRow.uri)
        if (!file.exists()) return@withContext null

        val ref = FileBackedRef(file, fileRow.displayName)
        val handler = formatRegistry.handlerFor(entity.toDomain().format)
            ?: formatRegistry.resolve(ref)?.handler
            ?: return@withContext null
        val content = runCatching { handler.openContent(ref) }.getOrNull() ?: return@withContext null
        OpenedBook(entity.toDomain(), content)
    }

    suspend fun loadProgress(bookId: Long): Locator? = withContext(io) {
        progressDao.getProgress(bookId)?.toLocator()
    }

    suspend fun saveProgress(bookId: Long, locator: Locator, chapterTitle: String?) = withContext(io) {
        progressDao.upsert(
            ReadingProgressEntity(
                bookId = bookId,
                spineIndex = locator.spineIndex,
                selector = locator.selector,
                charOffset = locator.charOffset,
                textSnippet = locator.textSnippet,
                pageIndex = locator.pageIndex,
                progression = locator.progression,
                chapterTitle = chapterTitle,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        bookDao.updateLastOpened(bookId, System.currentTimeMillis())
        // Mark as reading once opened, unless already finished/abandoned by the user.
        val state = bookDao.getBook(bookId)?.readingState
        if (state == ReadingState.UNREAD.name) {
            bookDao.updateReadingState(bookId, ReadingState.READING.name)
        }
    }
}

data class OpenedBook(val book: Book, val content: BookContent)

private fun ReadingProgressEntity.toLocator(): Locator = Locator(
    spineIndex = spineIndex,
    selector = selector,
    charOffset = charOffset,
    textSnippet = textSnippet,
    pageIndex = pageIndex,
    progression = progression,
)
