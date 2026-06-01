package com.dogear.reader.core.ingest

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.dogear.reader.core.common.dispatchers.IoDispatcher
import com.dogear.reader.core.common.io.Hashing
import com.dogear.reader.core.cover.CoverCache
import com.dogear.reader.core.database.dao.BookDao
import com.dogear.reader.core.database.entity.BookFileEntity
import com.dogear.reader.core.database.mapper.toEntity
import com.dogear.reader.core.model.Book
import com.dogear.reader.core.model.BookFormat
import com.dogear.reader.format.api.FileBackedRef
import com.dogear.reader.format.api.FormatRegistry
import com.dogear.reader.format.api.io.SafeZip
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import javax.inject.Inject

/**
 * Imports books from any source (file picker, folder tree, ZIP archive, or an upload stream).
 * The pipeline is: copy → hash → probe-by-content → metadata → cover/thumbnail → DB. It is
 * duplicate-safe (content hash) and applies the archive security limits from [SafeZip]
 * (Security Review §B). Everything runs off the main thread.
 */
class BookImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val formatRegistry: FormatRegistry,
    private val coverCache: CoverCache,
    private val bookDao: BookDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private val booksDir: File get() = File(context.filesDir, "books").apply { mkdirs() }

    suspend fun importFromUri(uri: Uri): ImportResult = withContext(io) {
        val display = queryDisplayName(uri)
        val temp = newTemp(display)
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { input.copyTo(it) }
            } ?: return@withContext ImportResult.Failed("Could not open file")
        } catch (t: Throwable) {
            temp.delete()
            return@withContext ImportResult.Failed(t.message ?: "Read error")
        }
        ingest(temp, display, allowArchive = true)
    }

    /** Imports from an upload/HTTP body stream. The caller provides the client-declared name. */
    suspend fun importFromStream(input: InputStream, displayName: String?): ImportResult =
        withContext(io) {
            val temp = newTemp(displayName)
            try {
                temp.outputStream().use { input.copyTo(it) }
            } catch (t: Throwable) {
                temp.delete()
                return@withContext ImportResult.Failed(t.message ?: "Read error")
            }
            ingest(temp, displayName, allowArchive = true)
        }

    suspend fun importUris(uris: List<Uri>): BatchImportResult = withContext(io) {
        uris.fold(BatchImportResult()) { acc, uri -> acc + importFromUri(uri) }
    }

    /** Recursively imports every supported book under a SAF tree (folder import). */
    suspend fun importTree(treeUri: Uri): BatchImportResult = withContext(io) {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext BatchImportResult()
        val files = mutableListOf<Uri>()
        collectFiles(root, files)
        importUris(files)
    }

    private fun collectFiles(dir: DocumentFile, out: MutableList<Uri>, depth: Int = 0) {
        if (depth > MAX_TREE_DEPTH) return
        dir.listFiles().forEach { entry ->
            when {
                entry.isDirectory -> collectFiles(entry, out, depth + 1)
                entry.isFile && isImportableName(entry.name) -> out += entry.uri
            }
        }
    }

    // --- core pipeline ---

    private suspend fun ingest(temp: File, displayName: String?, allowArchive: Boolean): ImportResult {
        val hash = runCatching { Hashing.sha256(temp.inputStream()) }.getOrElse {
            temp.delete()
            return ImportResult.Failed("Hashing failed")
        }
        if (bookDao.existsByHash(hash)) {
            temp.delete()
            return ImportResult.Duplicate
        }
        val ref = FileBackedRef(temp, displayName)
        val resolution = formatRegistry.resolve(ref)
        if (resolution == null) {
            // A plain ZIP that is not a recognized book is treated as an archive of books.
            if (allowArchive && SafeZip.isZip(temp)) return importArchive(temp)
            temp.delete()
            return ImportResult.Unsupported
        }
        return finalize(temp, displayName, hash, ref, resolution)
    }

    private suspend fun finalize(
        temp: File,
        displayName: String?,
        hash: String,
        ref: FileBackedRef,
        resolution: FormatRegistry.Resolution,
    ): ImportResult {
        val handler = resolution.handler
        val metadata = runCatching { handler.extractMetadata(ref) }.getOrNull()
        val title = metadata?.title?.takeIf { it.isNotBlank() }
            ?: displayName?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
            ?: "Untitled"
        val author = metadata?.authors?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Unknown"

        // Cover is extracted from the temp file before it is moved into place.
        val rawCover = runCatching { handler.extractCover(ref) }.getOrNull()
        val storedCover = rawCover?.let { coverCache.storeFromRaw(hash, it) }
        val coverPath = storedCover ?: coverCache.storeGenerated(hash, title, author)
        val coverGenerated = storedCover == null

        val ext = displayName?.substringAfterLast('.', "")?.lowercase()?.ifBlank { null }
            ?: resolution.format.extensions.firstOrNull()
            ?: "bin"
        val dest = File(booksDir, "$hash.$ext")
        runCatching {
            temp.copyTo(dest, overwrite = true)
            temp.delete()
        }.onFailure {
            temp.delete()
            return ImportResult.Failed("Storage error")
        }

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
        if (bookId <= 0) return ImportResult.Duplicate
        bookDao.insertFile(
            BookFileEntity(bookId = bookId, uri = dest.absolutePath, displayName = displayName, contentHash = hash),
        )
        return ImportResult.Imported(bookId)
    }

    /** Extracts and imports the supported books inside a ZIP, with per-entry size bounds. */
    private suspend fun importArchive(archive: File): ImportResult {
        var imported = 0
        var skipped = 0
        val names = runCatching { SafeZip.entryNames(archive) }.getOrDefault(emptyList())
        for (name in names.take(MAX_ARCHIVE_BOOKS)) {
            if (!isImportableName(name)) {
                skipped++
                continue
            }
            val bytes = SafeZip.readEntry(archive, name)
            if (bytes == null) {
                skipped++ // oversized/zip-bomb entry rejected by SafeZip
                continue
            }
            val entryTemp = newTemp(name.substringAfterLast('/'))
            entryTemp.writeBytes(bytes)
            // Nested archives are not re-expanded (allowArchive = false) to bound work.
            when (ingest(entryTemp, name.substringAfterLast('/'), allowArchive = false)) {
                is ImportResult.Imported -> imported++
                else -> skipped++
            }
        }
        archive.delete()
        return ImportResult.Archive(imported, skipped)
    }

    private fun newTemp(displayName: String?): File {
        val ext = displayName?.substringAfterLast('.', "")?.lowercase()?.ifBlank { null }
        return File.createTempFile("import-", ext?.let { ".$it" } ?: ".tmp", context.cacheDir)
    }

    private fun isImportableName(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in BOOK_EXTENSIONS
    }

    private fun queryDisplayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    }.getOrNull()

    private companion object {
        const val MAX_TREE_DEPTH = 8
        const val MAX_ARCHIVE_BOOKS = 2_000
        val BOOK_EXTENSIONS: Set<String> =
            BookFormat.entries.flatMap { it.extensions }.toSet()
    }
}
