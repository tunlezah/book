package com.dogear.reader.feature.library.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.dogear.reader.core.common.dispatchers.IoDispatcher
import com.dogear.reader.core.cover.CoverCache
import com.dogear.reader.core.database.dao.BookDao
import com.dogear.reader.core.database.entity.BookFileEntity
import com.dogear.reader.core.database.mapper.toEntity
import com.dogear.reader.core.model.Book
import com.dogear.reader.format.api.FileBackedRef
import com.dogear.reader.format.api.FormatRegistry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.security.DigestInputStream
import java.security.MessageDigest
import javax.inject.Inject

/**
 * Imports a single book end-to-end: copy → hash → probe → metadata → cover/thumbnail → DB.
 * This is the Phase-2 slice of the import system (full SAF folder/ZIP/web-upload arrives in
 * Phase 5). Everything runs off the main thread and is duplicate-safe via the content hash.
 */
class BookImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val formatRegistry: FormatRegistry,
    private val coverCache: CoverCache,
    private val bookDao: BookDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    suspend fun importFromUri(uri: Uri): ImportResult = withContext(io) {
        val display = queryDisplayName(uri)
        val ext = display?.substringAfterLast('.', "")?.lowercase()?.ifBlank { null }

        // Copy to a temp file while hashing, so we touch the SAF stream exactly once.
        val temp = File.createTempFile("import-", ext?.let { ".$it" } ?: ".tmp", context.cacheDir)
        val hash = try {
            copyAndHash(uri, temp)
        } catch (t: Throwable) {
            temp.delete()
            return@withContext ImportResult.Failed(t.message ?: "Could not read file")
        }

        if (bookDao.existsByHash(hash)) {
            temp.delete()
            return@withContext ImportResult.Duplicate
        }

        val probeRef = FileBackedRef(temp, display)
        val resolution = formatRegistry.resolve(probeRef) ?: run {
            temp.delete()
            return@withContext ImportResult.Unsupported
        }

        val handler = resolution.handler
        val metadata = runCatching { handler.extractMetadata(probeRef) }.getOrNull()
        val title = metadata?.title?.takeIf { it.isNotBlank() }
            ?: display?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
            ?: "Untitled"
        val author = metadata?.authors?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Unknown"

        // Move the verified file into permanent app storage, keyed by hash.
        val booksDir = File(context.filesDir, "books").apply { mkdirs() }
        val dest = File(booksDir, "$hash.${ext ?: resolution.format.extensions.firstOrNull() ?: "bin"}")
        temp.copyTo(dest, overwrite = true)
        temp.delete()
        val destRef = FileBackedRef(dest, display)

        val rawCover = runCatching { handler.extractCover(destRef) }.getOrNull()
        val storedCover = rawCover?.let { coverCache.storeFromRaw(hash, it) }
        val coverPath = storedCover ?: coverCache.storeGenerated(hash, title, author)
        val coverGenerated = storedCover == null

        val now = System.currentTimeMillis()
        val book = Book(
            contentHash = hash,
            title = title,
            author = author,
            authors = metadata?.authors ?: emptyList(),
            publisher = metadata?.publisher,
            publishedDate = metadata?.publishedDate,
            description = metadata?.description,
            isbn = metadata?.isbn,
            language = metadata?.language,
            series = metadata?.series,
            seriesIndex = metadata?.seriesIndex,
            categories = metadata?.categories ?: emptyList(),
            format = resolution.format,
            fileSizeBytes = dest.length(),
            coverPath = coverPath,
            coverIsGenerated = coverGenerated,
            addedAt = now,
        )
        val bookId = bookDao.insert(book.toEntity())
        if (bookId <= 0) {
            return@withContext ImportResult.Duplicate
        }
        bookDao.insertFile(
            BookFileEntity(
                bookId = bookId,
                uri = dest.absolutePath,
                displayName = display,
                contentHash = hash,
            ),
        )
        ImportResult.Imported(bookId)
    }

    private fun copyAndHash(uri: Uri, dest: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Unable to open ${uri}")
        DigestInputStream(input, digest).use { stream ->
            dest.outputStream().use { stream.copyTo(it) }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun queryDisplayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
    }.getOrNull()
}

sealed interface ImportResult {
    data class Imported(val bookId: Long) : ImportResult
    data object Duplicate : ImportResult
    data object Unsupported : ImportResult
    data class Failed(val reason: String) : ImportResult
}
