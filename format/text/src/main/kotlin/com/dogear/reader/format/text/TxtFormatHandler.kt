package com.dogear.reader.format.text

import com.dogear.reader.core.model.BookFormat
import com.dogear.reader.core.model.BookMetadata
import com.dogear.reader.format.api.BookFormatHandler
import com.dogear.reader.format.api.FileRef
import com.dogear.reader.format.api.ProbeResult
import com.dogear.reader.format.api.RawImage
import javax.inject.Inject

/**
 * Plain-text handler and the catch-all. Lowest priority so it only claims a file no
 * signature-based handler wanted, and only when the bytes actually look like text — preventing
 * it from swallowing unknown binaries. Covers are always generated (no embedded image).
 */
class TxtFormatHandler @Inject constructor() : BookFormatHandler {

    override val supportedFormats: Set<BookFormat> = setOf(BookFormat.TXT)
    override val priority: Int = 10

    override suspend fun probe(ref: FileRef): ProbeResult {
        val isTxtExt = ref.extension?.equals("txt", ignoreCase = true) == true
        if (isTxtExt) return ProbeResult.of(BookFormat.TXT)
        val header = runCatching {
            ref.openInputStream().use { input ->
                val buffer = ByteArray(SNIFF_BYTES)
                val read = input.read(buffer)
                if (read <= 0) ByteArray(0) else buffer.copyOf(read)
            }
        }.getOrNull() ?: return ProbeResult.NoMatch
        return if (looksLikeText(header)) ProbeResult.of(BookFormat.TXT) else ProbeResult.NoMatch
    }

    override suspend fun extractMetadata(ref: FileRef): BookMetadata {
        // Use the first non-blank line as a title hint; the importer falls back to the filename.
        val firstLine = runCatching {
            ref.openInputStream().bufferedReader().useLines { lines ->
                lines.firstOrNull { it.isNotBlank() }?.trim()?.take(MAX_TITLE)
            }
        }.getOrNull()
        return BookMetadata(title = firstLine)
    }

    override suspend fun extractCover(ref: FileRef): RawImage? = null

    private fun looksLikeText(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false
        // UTF-8/16 BOMs are clearly text.
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) return true
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte()) return true
        var control = 0
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            if (v == 0) return false // NUL ⇒ binary
            val isControl = v < 0x09 || (v in 0x0E..0x1F)
            if (isControl) control++
        }
        return control.toDouble() / bytes.size < 0.05
    }

    private companion object {
        const val SNIFF_BYTES = 2048
        const val MAX_TITLE = 120
    }
}
