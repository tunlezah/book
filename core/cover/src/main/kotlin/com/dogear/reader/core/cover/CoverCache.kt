package com.dogear.reader.core.cover

import android.content.Context
import android.graphics.Bitmap
import com.dogear.reader.core.common.dispatchers.IoDispatcher
import com.dogear.reader.format.api.RawImage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistent, content-hash-keyed cover/thumbnail cache. Covers are generated once and reused
 * forever (keyed by the book's content hash), so re-imports never regenerate and shelf startup
 * only reads small files. Stored as downsampled JPEGs to keep the cache small and decoding fast
 * (Cover Management + Performance strategy).
 */
@Singleton
class CoverCache @Inject constructor(
    @ApplicationContext context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private val dir: File = File(context.filesDir, "covers").apply { mkdirs() }

    fun coverFile(hash: String): File = File(dir, "$hash.jpg")

    fun pathFor(hash: String): String? = coverFile(hash).takeIf { it.exists() }?.absolutePath

    /** Decodes, downsamples, and stores an embedded cover. Returns the path, or null on failure. */
    suspend fun storeFromRaw(hash: String, raw: RawImage): String? = withContext(io) {
        val target = coverFile(hash)
        if (target.exists()) return@withContext target.absolutePath
        val bitmap = ThumbnailDecoder.decodeSampled(raw.bytes, THUMB_MAX_SIDE) ?: return@withContext null
        writeJpeg(bitmap, target)
        bitmap.recycle()
        target.absolutePath
    }

    /** Generates and stores a typographic cover from title/author. Returns the path. */
    suspend fun storeGenerated(hash: String, title: String, author: String): String = withContext(io) {
        val target = coverFile(hash)
        if (target.exists()) return@withContext target.absolutePath
        val bitmap = GeneratedCover.create(title, author)
        writeJpeg(bitmap, target)
        bitmap.recycle()
        target.absolutePath
    }

    fun delete(hash: String) {
        coverFile(hash).delete()
    }

    fun clearAll() {
        dir.listFiles()?.forEach { it.delete() }
    }

    fun totalBytes(): Long = dir.listFiles()?.sumOf { it.length() } ?: 0L

    private fun writeJpeg(bitmap: Bitmap, target: File) {
        target.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out) }
    }

    private companion object {
        const val THUMB_MAX_SIDE = 512
    }
}
