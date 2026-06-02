package com.dogear.reader.format.api.io

import com.dogear.reader.format.api.FileRef

/** Magic-byte sniffing so detection is by content, not by (spoofable) extension. */
object Magic {

    fun header(ref: FileRef, count: Int = 8): ByteArray = runCatching {
        ref.openInputStream().use { input ->
            val buffer = ByteArray(count)
            val read = input.read(buffer)
            if (read <= 0) ByteArray(0) else buffer.copyOf(read)
        }
    }.getOrDefault(ByteArray(0))

    fun startsWith(bytes: ByteArray, vararg prefix: Int): Boolean {
        if (bytes.size < prefix.size) return false
        for (i in prefix.indices) {
            if (bytes[i] != prefix[i].toByte()) return false
        }
        return true
    }

    /** "%PDF-" */
    fun isPdf(bytes: ByteArray): Boolean = startsWith(bytes, 0x25, 0x50, 0x44, 0x46, 0x2D)

    /** Local file header "PK". */
    fun isZip(bytes: ByteArray): Boolean = startsWith(bytes, 0x50, 0x4B, 0x03, 0x04)
}
