package com.dogear.reader.format.api

import com.dogear.reader.core.model.BookFormat
import com.dogear.reader.core.model.BookMetadata
import com.dogear.reader.format.api.content.BookContent
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

class FormatRegistryTest {

    @Test
    fun resolve_prefersHigherPriorityHandler() = runTest {
        val low = FakeHandler(priority = 10, matches = true, format = BookFormat.TXT)
        val high = FakeHandler(priority = 100, matches = true, format = BookFormat.EPUB)
        val registry = FormatRegistry(setOf(low, high))

        val resolution = registry.resolve(FakeRef())

        assertThat(resolution?.format).isEqualTo(BookFormat.EPUB)
        assertThat(resolution?.handler).isEqualTo(high)
    }

    @Test
    fun resolve_returnsNullWhenNothingMatches() = runTest {
        val registry = FormatRegistry(setOf(FakeHandler(priority = 50, matches = false)))
        assertThat(registry.resolve(FakeRef())).isNull()
    }

    @Test
    fun resolve_isResilientToHandlerThatThrows() = runTest {
        val throwing = object : FakeHandler(priority = 100, matches = true, format = BookFormat.PDF) {
            override suspend fun probe(ref: FileRef): ProbeResult = throw RuntimeException("boom")
        }
        val good = FakeHandler(priority = 10, matches = true, format = BookFormat.TXT)
        val registry = FormatRegistry(setOf(throwing, good))

        assertThat(registry.resolve(FakeRef())?.format).isEqualTo(BookFormat.TXT)
    }

    private open class FakeHandler(
        override val priority: Int,
        private val matches: Boolean,
        private val format: BookFormat = BookFormat.UNKNOWN,
    ) : BookFormatHandler {
        override val supportedFormats: Set<BookFormat> = setOf(format)
        override suspend fun probe(ref: FileRef): ProbeResult =
            if (matches) ProbeResult.of(format) else ProbeResult.NoMatch
        override suspend fun extractMetadata(ref: FileRef): BookMetadata = BookMetadata.Empty
        override suspend fun extractCover(ref: FileRef): RawImage? = null
        override suspend fun openContent(ref: FileRef): BookContent =
            object : BookContent.ImagePager {
                override val pageCount: Int = 0
                override suspend fun page(index: Int, target: android.util.Size) = null
            }
    }

    private class FakeRef : FileRef {
        override val displayName: String? = "x.bin"
        override val extension: String? = "bin"
        override val length: Long = 0
        override fun openInputStream(): InputStream = ByteArrayInputStream(ByteArray(0))
        override fun asFile() = null
    }
}
