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
import com.dogear.reader.format.api.content.DrmProtectedException
import com.dogear.reader.format.api.content.PasswordRequiredException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
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
    suspend fun open(bookId: Long): OpenOutcome = withContext(io) {
        val entity = bookDao.getBook(bookId)
            ?: return@withContext OpenOutcome.Failure("This book is no longer in your library.")
        val fileRow = bookDao.getPrimaryFile(bookId)
            ?: return@withContext OpenOutcome.Failure("This book's file is missing — re-import it to read.")
        val file = File(fileRow.uri)
        if (!file.exists()) {
            return@withContext OpenOutcome.Failure("This book's file is missing — re-import it to read.")
        }

        val ref = FileBackedRef(file, fileRow.displayName)
        val handler = formatRegistry.handlerFor(entity.toDomain().format)
            ?: formatRegistry.resolve(ref)?.handler
            ?: return@withContext OpenOutcome.Failure("This file format isn't supported yet.")

        // Classify failures so the reader can show a clear, specific message (never a crash).
        try {
            val content = handler.openContent(ref)
            OpenOutcome.Success(OpenedBook(entity.toDomain(), content))
        } catch (e: DrmProtectedException) {
            OpenOutcome.Failure(e.message ?: "This book is DRM-protected.")
        } catch (e: PasswordRequiredException) {
            OpenOutcome.Failure(e.message ?: "This file is password-protected.")
        } catch (e: SecurityException) {
            OpenOutcome.Failure("This file is password-protected and can't be opened here.")
        } catch (e: IOException) {
            OpenOutcome.Failure("This file appears to be corrupt or unreadable.")
        } catch (t: Throwable) {
            OpenOutcome.Failure("Couldn't open this book.")
        }
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

/** Outcome of opening a book — a success with content, or a failure with a user-facing message. */
sealed interface OpenOutcome {
    data class Success(val opened: OpenedBook) : OpenOutcome
    data class Failure(val message: String) : OpenOutcome
}

private fun ReadingProgressEntity.toLocator(): Locator = Locator(
    spineIndex = spineIndex,
    selector = selector,
    charOffset = charOffset,
    textSnippet = textSnippet,
    pageIndex = pageIndex,
    progression = progression,
)
