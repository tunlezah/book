package com.dogear.reader.format.api

import java.io.File
import java.io.InputStream

/**
 * Abstraction over a book's bytes, decoupling handlers from where the file lives (app storage,
 * SAF content URI, test fixture). Handlers that need random access (ZIP central directory,
 * PdfRenderer's seekable descriptor) call [asFile] / [materialize].
 */
interface FileRef {
    val displayName: String?
    val extension: String?
    val length: Long

    fun openInputStream(): InputStream

    /** The backing file when one exists, else null (e.g. a pure stream source). */
    fun asFile(): File?
}

/** Returns a real file for random-access readers, copying to a temp file only if necessary. */
fun FileRef.materialize(): File {
    asFile()?.let { return it }
    val temp = File.createTempFile("dogear-", extensionSuffix())
    temp.deleteOnExit()
    openInputStream().use { input -> temp.outputStream().use { input.copyTo(it) } }
    return temp
}

private fun FileRef.extensionSuffix(): String =
    extension?.takeIf { it.isNotBlank() }?.let { ".${it.removePrefix(".")}" } ?: ".bin"

/** A [FileRef] backed by a real [File] on disk. */
class FileBackedRef(
    private val file: File,
    override val displayName: String? = file.name,
) : FileRef {
    override val extension: String? = file.extension.ifBlank { null }
    override val length: Long get() = file.length()
    override fun openInputStream(): InputStream = file.inputStream()
    override fun asFile(): File = file
}
