package com.dogear.reader.format.text

import com.dogear.reader.format.api.FileBackedRef
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File

class TxtFormatHandlerTest {

    private val handler = TxtFormatHandler()

    @Test
    fun probe_acceptsTxtExtension() = runTest {
        val file = tempFile("notes.txt", "hello world".toByteArray())
        assertThat(handler.probe(FileBackedRef(file)).matches).isTrue()
    }

    @Test
    fun probe_acceptsPlainTextWithoutExtension() = runTest {
        val file = tempFile("plain", "Just some readable prose.\nA second line.".toByteArray())
        assertThat(handler.probe(FileBackedRef(file)).matches).isTrue()
    }

    @Test
    fun probe_rejectsBinary() = runTest {
        val binary = byteArrayOf(0x00, 0x01, 0x02, 0x00, 0x7F, 0x00)
        val file = tempFile("blob", binary)
        assertThat(handler.probe(FileBackedRef(file)).matches).isFalse()
    }

    @Test
    fun metadata_usesFirstNonBlankLineAsTitle() = runTest {
        val file = tempFile("story.txt", "\n\nThe Folded Page\nby someone\n".toByteArray())
        assertThat(handler.extractMetadata(FileBackedRef(file)).title).isEqualTo("The Folded Page")
    }

    private fun tempFile(name: String, bytes: ByteArray): File {
        val dir = File(System.getProperty("java.io.tmpdir"), "dogear-txt-test").apply { mkdirs() }
        return File(dir, "${System.nanoTime()}-$name").apply { writeBytes(bytes) }
    }
}
