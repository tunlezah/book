package com.dogear.reader.core.common.io

import java.io.InputStream
import java.security.MessageDigest

/**
 * Content hashing for duplicate detection and cache keys. A book is identified by the SHA-256
 * of its bytes so re-imports reuse cached covers/thumbnails and never duplicate (see Cover
 * Management + Performance strategy).
 */
object Hashing {

    private const val BUFFER_SIZE = 64 * 1024

    fun sha256(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(BUFFER_SIZE)
        var read = input.read(buffer)
        while (read >= 0) {
            digest.update(buffer, 0, read)
            read = input.read(buffer)
        }
        return digest.digest().toHex()
    }

    fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

    private fun ByteArray.toHex(): String =
        joinToString(separator = "") { byte -> "%02x".format(byte) }
}
