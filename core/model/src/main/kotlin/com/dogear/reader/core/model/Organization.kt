package com.dogear.reader.core.model

/** A user-created folder of books. */
data class Collection(
    val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0,
    val createdAt: Long = 0,
)

/** A free-form label that can be applied to many books. */
data class Tag(
    val id: Long = 0,
    val name: String,
)
