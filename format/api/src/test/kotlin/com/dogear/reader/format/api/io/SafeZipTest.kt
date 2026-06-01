package com.dogear.reader.format.api.io

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SafeZipTest {

    @Test
    fun entryNames_useNaturalOrder() {
        val file = zip("page10.jpg", "page2.jpg", "page1.jpg")
        assertThat(SafeZip.entryNames(file))
            .containsExactly("page1.jpg", "page2.jpg", "page10.jpg").inOrder()
    }

    @Test
    fun readEntry_returnsBytes() {
        val file = zip("a.txt" to "hello")
        assertThat(SafeZip.readEntry(file, "a.txt")?.toString(Charsets.UTF_8)).isEqualTo("hello")
    }

    @Test
    fun readEntry_oversizedEntryIsRejected() {
        val big = "x".repeat(2048)
        val file = zip("big.txt" to big)
        val tinyLimit = SafeZip.Limits(maxEntryBytes = 100)
        assertThat(SafeZip.readEntry(file, "big.txt", tinyLimit)).isNull()
    }

    @Test
    fun isZip_detectsSignature() {
        assertThat(SafeZip.isZip(zip("x.txt" to "y"))).isTrue()
        val notZip = File.createTempFile("plain", ".bin").apply { writeText("not a zip") }
        assertThat(SafeZip.isZip(notZip)).isFalse()
    }

    private fun zip(vararg names: String): File =
        zip(*names.map { it to "data" }.toTypedArray())

    private fun zip(vararg entries: Pair<String, String>): File {
        val file = File.createTempFile("safe", ".zip")
        ZipOutputStream(file.outputStream()).use { out ->
            entries.forEach { (name, body) ->
                out.putNextEntry(ZipEntry(name))
                out.write(body.toByteArray())
                out.closeEntry()
            }
        }
        return file
    }
}
