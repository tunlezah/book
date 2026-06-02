package com.dogear.reader.core.ingest

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.dogear.reader.core.cover.CoverCache
import com.dogear.reader.core.database.DogearDatabase
import com.dogear.reader.format.api.FormatRegistry
import com.dogear.reader.format.text.TxtFormatHandler
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BookImporterTest {

    private lateinit var db: DogearDatabase
    private lateinit var importer: BookImporter

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, DogearDatabase::class.java)
            .allowMainThreadQueries().build()
        val dispatcher = UnconfinedTestDispatcher()
        importer = BookImporter(
            context = context,
            formatRegistry = FormatRegistry(setOf(TxtFormatHandler())),
            coverCache = CoverCache(context, dispatcher),
            bookDao = db.bookDao(),
            io = dispatcher,
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun importStream_addsBookWithGeneratedCover() = runTest {
        val bytes = "The Folded Page\n\nA quiet beginning.".toByteArray()
        val result = importer.importFromStream(bytes.inputStream(), "the-folded-page.txt")

        assertThat(result).isInstanceOf(ImportResult.Imported::class.java)
        val id = (result as ImportResult.Imported).bookId
        val book = db.bookDao().getBook(id)
        assertThat(book).isNotNull()
        assertThat(book!!.coverIsGenerated).isTrue()
        assertThat(db.bookDao().getPrimaryFile(id)).isNotNull()
    }

    @Test
    fun importStream_dedupesIdenticalContent() = runTest {
        val bytes = "Same content".toByteArray()
        val first = importer.importFromStream(bytes.inputStream(), "a.txt")
        val second = importer.importFromStream(bytes.inputStream(), "b.txt")

        assertThat(first).isInstanceOf(ImportResult.Imported::class.java)
        assertThat(second).isEqualTo(ImportResult.Duplicate)
    }

    @Test
    fun importStream_rejectsBinaryAsUnsupported() = runTest {
        val binary = byteArrayOf(0, 1, 2, 0, 3, 0)
        val result = importer.importFromStream(binary.inputStream(), "blob.bin")
        assertThat(result).isEqualTo(ImportResult.Unsupported)
    }
}
