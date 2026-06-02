package com.dogear.reader.format.comic

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
class CbzFormatHandlerTest {

    private val handler = CbzFormatHandler()

    @Test
    fun cover_isFirstImageInNaturalOrder() = runTest {
        // Out-of-order names ensure natural sort puts page1 before page10.
        val file = cbz(
            "page10.jpg" to byteArrayOf(10),
            "page2.jpg" to byteArrayOf(2),
            "page1.jpg" to byteArrayOf(1),
        )
        val cover = handler.extractCover(FileBackedRef(file))
        assertThat(cover).isNotNull()
        assertThat(cover!!.bytes).isEqualTo(byteArrayOf(1))
    }

    @Test
    fun probe_matchesZipWithImages() = runTest {
        val file = cbz("001.png" to byteArrayOf(1))
        assertThat(handler.probe(FileBackedRef(file)).matches).isTrue()
    }

    private fun cbz(vararg entries: Pair<String, ByteArray>): File {
        val file = File.createTempFile("sample", ".cbz")
        ZipOutputStream(file.outputStream()).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return file
    }
}
