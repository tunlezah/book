package com.dogear.reader.format.epub

import com.dogear.reader.core.model.BookFormat
import com.dogear.reader.format.api.FileBackedRef
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
class EpubFormatHandlerTest {

    private val handler = EpubFormatHandler()

    @Test
    fun probe_detectsEpub3() = runTest {
        val ref = FileBackedRef(sampleEpub())
        val result = handler.probe(ref)
        assertThat(result.matches).isTrue()
        assertThat(result.format).isEqualTo(BookFormat.EPUB3)
    }

    @Test
    fun extractMetadata_readsOpf() = runTest {
        val metadata = handler.extractMetadata(FileBackedRef(sampleEpub()))
        assertThat(metadata.title).isEqualTo("The Quiet Library")
        assertThat(metadata.authors).containsExactly("Ada Vance")
        assertThat(metadata.language).isEqualTo("en")
    }

    @Test
    fun extractCover_returnsImageBytes() = runTest {
        val cover = handler.extractCover(FileBackedRef(sampleEpub()))
        assertThat(cover).isNotNull()
        assertThat(cover!!.bytes).isEqualTo(PNG_BYTES)
        assertThat(cover.mimeType).isEqualTo("image/png")
    }

    @Test
    fun probe_rejectsNonEpubZip() = runTest {
        val file = File.createTempFile("plain", ".zip").apply {
            ZipOutputStream(outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("hello.txt"))
                zip.write("hi".toByteArray())
                zip.closeEntry()
            }
        }
        assertThat(handler.probe(FileBackedRef(file)).matches).isFalse()
    }

    private fun sampleEpub(): File {
        val file = File.createTempFile("sample", ".epub")
        ZipOutputStream(file.outputStream()).use { zip ->
            zip.write("mimetype", "application/epub+zip".toByteArray())
            zip.write("META-INF/container.xml", CONTAINER.toByteArray())
            zip.write("OEBPS/content.opf", OPF.toByteArray())
            zip.write("OEBPS/images/cover.png", PNG_BYTES)
        }
        return file
    }

    private fun ZipOutputStream.write(name: String, bytes: ByteArray) {
        putNextEntry(ZipEntry(name))
        write(bytes)
        closeEntry()
    }

    private companion object {
        val PNG_BYTES = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3)

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
                <dc:title>The Quiet Library</dc:title>
                <dc:creator>Ada Vance</dc:creator>
                <dc:language>en</dc:language>
                <meta name="cover" content="cover-img"/>
              </metadata>
              <manifest>
                <item id="cover-img" href="images/cover.png" media-type="image/png" properties="cover-image"/>
              </manifest>
            </package>
        """.trimIndent()
    }
}
