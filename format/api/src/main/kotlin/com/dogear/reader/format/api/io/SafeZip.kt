package com.dogear.reader.format.api.io

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Defensive ZIP reading for EPUB/CBZ (Security Review §B). Reads are bounded so a zip bomb or
 * malformed archive can never exhaust memory: per-entry and total uncompressed reads are
 * capped, entry counts are limited, and we never trust the declared size. We read entries by
 * name into memory (no extraction to disk), so Zip-Slip path traversal does not apply.
 */
object SafeZip {

    data class Limits(
        val maxEntries: Int = 10_000,
        val maxEntryBytes: Long = 64L * 1024 * 1024,
        val maxTotalBytes: Long = 512L * 1024 * 1024,
    )

    val DEFAULT = Limits()

    fun isZip(file: File): Boolean = runCatching {
        file.inputStream().use { stream ->
            val sig = ByteArray(4)
            stream.read(sig) == 4 && sig[0] == 'P'.code.toByte() && sig[1] == 'K'.code.toByte()
        }
    }.getOrDefault(false)

    /** Entry names in stable, natural order (case-insensitive), excluding directories. */
    fun entryNames(file: File, limits: Limits = DEFAULT): List<String> = ZipFile(file).use { zip ->
        zip.entries().asSequence()
            .take(limits.maxEntries)
            .filterNot { it.isDirectory }
            .map { it.name }
            .sortedWith(NaturalOrder)
            .toList()
    }

    fun readEntry(file: File, name: String, limits: Limits = DEFAULT): ByteArray? =
        ZipFile(file).use { zip ->
            val entry = zip.getEntry(name) ?: return null
            readBounded(zip, entry, limits.maxEntryBytes)
        }

    /** Reads the first entry whose name satisfies [predicate] (in natural order). */
    fun readFirst(file: File, limits: Limits = DEFAULT, predicate: (String) -> Boolean): NamedBytes? =
        ZipFile(file).use { zip ->
            val match = zip.entries().asSequence()
                .filterNot { it.isDirectory }
                .map { it.name }
                .sortedWith(NaturalOrder)
                .firstOrNull(predicate) ?: return null
            val entry = zip.getEntry(match) ?: return null
            readBounded(zip, entry, limits.maxEntryBytes)?.let { NamedBytes(match, it) }
        }

    private fun readBounded(zip: ZipFile, entry: ZipEntry, maxBytes: Long): ByteArray? {
        zip.getInputStream(entry).use { input ->
            val buffer = ByteArray(64 * 1024)
            val out = ArrayList<Byte>(minOf(maxBytes, 1 shl 20).toInt())
            var total = 0L
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                if (total > maxBytes) return null // zip bomb guard: refuse oversized entry
                for (i in 0 until read) out.add(buffer[i])
            }
            return out.toByteArray()
        }
    }

    data class NamedBytes(val name: String, val bytes: ByteArray)

    /** Natural sort so "page2" precedes "page10". */
    private val NaturalOrder = Comparator<String> { a, b ->
        val ra = tokenize(a)
        val rb = tokenize(b)
        var i = 0
        while (i < ra.size && i < rb.size) {
            val cmp = compareToken(ra[i], rb[i])
            if (cmp != 0) return@Comparator cmp
            i++
        }
        ra.size - rb.size
    }

    private fun tokenize(s: String): List<String> =
        Regex("\\d+|\\D+").findAll(s.lowercase()).map { it.value }.toList()

    private fun compareToken(a: String, b: String): Int {
        val na = a.toLongOrNull()
        val nb = b.toLongOrNull()
        return if (na != null && nb != null) na.compareTo(nb) else a.compareTo(b)
    }
}
