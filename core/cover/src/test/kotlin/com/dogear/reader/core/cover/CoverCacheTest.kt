package com.dogear.reader.core.cover

import androidx.test.core.app.ApplicationProvider
import com.dogear.reader.format.api.RawImage
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CoverCacheTest {

    @Test
    fun storeGenerated_writesPersistentFileAndReuses() = runTest {
        val cache = CoverCache(ApplicationProvider.getApplicationContext(), UnconfinedTestDispatcher(testScheduler))
        val first = cache.storeGenerated("hash-1", "Title", "Author")
        assertThat(java.io.File(first).exists()).isTrue()

        // A second call for the same hash reuses the file (no regeneration).
        val second = cache.storeGenerated("hash-1", "Different", "Other")
        assertThat(second).isEqualTo(first)
        assertThat(cache.pathFor("hash-1")).isEqualTo(first)
    }

    @Test
    fun storeFromRaw_failsGracefullyOnNonImageBytes() = runTest {
        val cache = CoverCache(ApplicationProvider.getApplicationContext(), UnconfinedTestDispatcher(testScheduler))
        val result = cache.storeFromRaw("hash-2", RawImage(byteArrayOf(1, 2, 3), "image/png"))
        assertThat(result).isNull()
    }
}
