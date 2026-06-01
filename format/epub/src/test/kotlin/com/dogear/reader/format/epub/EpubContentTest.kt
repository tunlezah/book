package com.dogear.reader.format.epub

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EpubContentTest {

    @Test
    fun spine_listsItemrefsInOrder() = runTest {
        val content = EpubContent(sampleEpub())
        val spine = content.spine()
        assertThat(spine.map { it.href }).containsExactly(
            "OEBPS/ch1.xhtml", "OEBPS/ch2.xhtml",
        ).inOrder()
    }

    @Test
    fun document_stripsScriptsAndHandlers() = runTest {
        val html = EpubContent(sampleEpub()).document(0).html
        assertThat(html).contains("Hello world")
        assertThat(html.lowercase()).doesNotContain("<script")
        assertThat(html.lowercase()).doesNotContain("onclick")
        assertThat(html).doesNotContain("alert(")
    }

    @Test
    fun toc_parsesNavEntries() = runTest {
        val toc = EpubContent(sampleEpub()).toc()
        assertThat(toc.map { it.title }).containsExactly("Chapter One", "Chapter Two").inOrder()
        assertThat(toc.first().href).isEqualTo("OEBPS/ch1.xhtml")
    }

    @Test
    fun resource_returnsBytesByRootPath() = runTest {
        val resource = EpubContent(sampleEpub()).resource("OEBPS/ch2.xhtml")
        assertThat(resource).isNotNull()
        assertThat(resource!!.mimeType).isEqualTo("text/html")
    }

    private fun sampleEpub(): File {
        val file = File.createTempFile("content", ".epub")
        ZipOutputStream(file.outputStream()).use { zip ->
            zip.put("mimetype", "application/epub+zip")
            zip.put("META-INF/container.xml", CONTAINER)
            zip.put("OEBPS/content.opf", OPF)
            zip.put(
                "OEBPS/ch1.xhtml",
                "<html><head><title>C1</title></head><body>" +
                    "<script>alert(1)</script><p onclick=\"x()\">Hello world</p></body></html>",
            )
            zip.put("OEBPS/ch2.xhtml", "<html><body><p>Second</p></body></html>")
            zip.put("OEBPS/nav.xhtml", NAV)
        }
        return file
    }

    private fun ZipOutputStream.put(name: String, body: String) {
        putNextEntry(ZipEntry(name))
        write(body.toByteArray())
        closeEntry()
    }

    private companion object {
        val CONTAINER = """
            <?xml version="1.0"?>
            <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
              <rootfiles>
                <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
              </rootfiles>
            </container>
        """.trimIndent()

        val OPF = """
            <?xml version="1.0"?>
            <package version="3.0" xmlns="http://www.idpf.org/2007/opf">
              <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title>Sample</dc:title>
              </metadata>
              <manifest>
                <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
                <item id="c1" href="ch1.xhtml" media-type="application/xhtml+xml"/>
                <item id="c2" href="ch2.xhtml" media-type="application/xhtml+xml"/>
              </manifest>
              <spine>
                <itemref idref="c1"/>
                <itemref idref="c2"/>
              </spine>
            </package>
        """.trimIndent()

        val NAV = """
            <html xmlns:epub="http://www.idpf.org/2007/ops">
              <body>
                <nav epub:type="toc">
                  <ol>
                    <li><a href="ch1.xhtml">Chapter One</a></li>
                    <li><a href="ch2.xhtml">Chapter Two</a></li>
                  </ol>
                </nav>
              </body>
            </html>
        """.trimIndent()
    }
}
