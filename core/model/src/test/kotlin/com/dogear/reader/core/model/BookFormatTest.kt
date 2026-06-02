package com.dogear.reader.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BookFormatTest {

    @Test
    fun fromExtension_resolvesKnownFormats() {
        assertThat(BookFormat.fromExtension("epub")).isEqualTo(BookFormat.EPUB)
        assertThat(BookFormat.fromExtension(".PDF")).isEqualTo(BookFormat.PDF)
        assertThat(BookFormat.fromExtension("cbz")).isEqualTo(BookFormat.CBZ)
        assertThat(BookFormat.fromExtension("htm")).isEqualTo(BookFormat.HTML)
    }

    @Test
    fun fromExtension_unknownOrBlankFallsBack() {
        assertThat(BookFormat.fromExtension("xyz")).isEqualTo(BookFormat.UNKNOWN)
        assertThat(BookFormat.fromExtension(null)).isEqualTo(BookFormat.UNKNOWN)
        assertThat(BookFormat.fromExtension("")).isEqualTo(BookFormat.UNKNOWN)
    }

    @Test
    fun tierOneFormatsAreThePromisedLaunchSet() {
        val tierOne = BookFormat.entries.filter { it.tier == 1 }.toSet()
        assertThat(tierOne).containsExactly(
            BookFormat.EPUB, BookFormat.EPUB3, BookFormat.PDF, BookFormat.TXT, BookFormat.CBZ,
        )
    }
}
