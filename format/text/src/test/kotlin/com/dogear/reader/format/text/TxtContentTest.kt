package com.dogear.reader.format.text

import com.dogear.reader.format.api.FileBackedRef
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File

class TxtContentTest {

    @Test
    fun document_wrapsParagraphsAndEscapesHtml() = runTest {
        val file = tempFile("First paragraph.\n\nSecond & <dangerous> paragraph.")
        val html = TxtContent(FileBackedRef(file)).document(0).html

        assertThat(html).contains("<p>First paragraph.</p>")
        assertThat(html).contains("Second &amp; &lt;dangerous&gt; paragraph.")
        assertThat(html).doesNotContain("<dangerous>")
    }

    @Test
    fun spine_isSingleDocument() = runTest {
        val content = TxtContent(FileBackedRef(tempFile("hello")))
        assertThat(content.spine()).hasSize(1)
        assertThat(content.toc()).isEmpty()
    }

    private fun tempFile(text: String): File {
        val dir = File(System.getProperty("java.io.tmpdir"), "dogear-txtcontent").apply { mkdirs() }
        return File(dir, "${System.nanoTime()}.txt").apply { writeText(text) }
    }
}
