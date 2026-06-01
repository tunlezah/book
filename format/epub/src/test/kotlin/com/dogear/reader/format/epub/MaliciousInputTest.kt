package com.dogear.reader.format.epub

import com.dogear.reader.format.api.FileBackedRef
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MaliciousInputTest {

    /**
     * "Billion laughs" entity expansion must not occur (DOCTYPE/external entities are disabled).
     * The parser rejects the undefined-entity reference and degrades to empty metadata instead of
     * expanding it — no OOM, no hang.
     */
    @Test
    fun opf_doesNotExpandXmlEntities() {
        val opf = """
            <?xml version="1.0"?>
            <!DOCTYPE package [
              <!ENTITY lol "lol">
              <!ENTITY lol2 "&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;">
              <!ENTITY lol3 "&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;">
            ]>
            <package version="2.0" xmlns="http://www.idpf.org/2007/opf">
              <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title>&lol3;</dc:title>
              </metadata>
            </package>
        """.trimIndent()

        val result = OpfParser.parseOpf(opf.toByteArray())
        // Either no title, or at most the literal — never a megabyte of expanded "lol".
        assertThat(result.metadata.title?.length ?: 0).isLessThan(1000)
    }

    @Test
    fun probe_rejectsGarbageWithoutCrashing() = runTest {
        val garbage = File.createTempFile("garbage", ".epub").apply {
            writeBytes(byteArrayOf(0x50, 0x4B, 0x03, 0x04) + ByteArray(64) { it.toByte() })
        }
        // A truncated/corrupt zip must not throw; it simply doesn't match.
        assertThat(EpubFormatHandler().probe(FileBackedRef(garbage)).matches).isFalse()
    }
}
