package com.dogear.reader.core.database.entity

import androidx.room.Entity
import androidx.room.Fts4

/**
 * Full-text search mirror of [BookEntity] for instant library search. Using `contentEntity`
 * makes this an external-content FTS table: it stores no duplicate data and stays in sync via
 * the triggers Room generates. The FTS rowid equals the book id, so search results join back
 * to `books` directly.
 */
@Fts4(contentEntity = BookEntity::class)
@Entity(tableName = "books_fts")
data class BookFtsEntity(
    val title: String,
    val author: String,
    val description: String?,
    val series: String?,
    val categories: String?,
)
