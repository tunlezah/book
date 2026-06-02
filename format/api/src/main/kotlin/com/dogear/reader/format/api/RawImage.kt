package com.dogear.reader.format.api

/** Raw cover image bytes as extracted from a book, before decoding/thumbnailing. */
class RawImage(
    val bytes: ByteArray,
    val mimeType: String? = null,
)
