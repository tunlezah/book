package com.dogear.reader.core.common.io

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HashingTest {

    @Test
    fun sha256_matchesKnownVector() {
        // SHA-256("abc")
        val expected = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
        assertThat(Hashing.sha256("abc".toByteArray())).isEqualTo(expected)
    }

    @Test
    fun sha256_streamAndBytesAgree() {
        val data = "The quick brown fox".toByteArray()
        val fromBytes = Hashing.sha256(data)
        val fromStream = Hashing.sha256(data.inputStream())
        assertThat(fromStream).isEqualTo(fromBytes)
    }
}
