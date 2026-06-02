package com.dogear.reader.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted book row. Column names mirror the Database Schema document. Indices back the
 * shelf's sort/filter/search so list queries never table-scan. Enum-valued columns
 * (`format`, `reading_state`) are stored as their stable name strings and mapped in
 * [com.dogear.reader.core.database.mapper] — avoiding a converter keeps the schema legible.
 *
 * FTS content columns (`title`, `author`, `description`, `series`, `categories`) are named to
 * match [BookFtsEntity] so the FTS table can use this entity as external content.
 */
@Entity(
    tableName = "books",
    indices = [
        Index(value = ["content_hash"], unique = true),
        Index(value = ["title"]),
        Index(value = ["author"]),
        Index(value = ["reading_state"]),
        Index(value = ["last_opened_at"]),
        Index(value = ["added_at"]),
        Index(value = ["format"]),
    ],
)
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "content_hash") val contentHash: String,
    val title: String,
    val subtitle: String? = null,
    val author: String,
    @ColumnInfo(name = "authors_json") val authorsJson: String? = null,
    val publisher: String? = null,
    @ColumnInfo(name = "published_date") val publishedDate: String? = null,
    val description: String? = null,
    val isbn: String? = null,
    val language: String? = null,
    val series: String? = null,
    @ColumnInfo(name = "series_index") val seriesIndex: Double? = null,
    val categories: String? = null,
    val format: String,
    @ColumnInfo(name = "file_size") val fileSize: Long = 0,
    @ColumnInfo(name = "cover_path") val coverPath: String? = null,
    @ColumnInfo(name = "cover_is_generated") val coverIsGenerated: Boolean = false,
    @ColumnInfo(name = "reading_state") val readingState: String,
    @ColumnInfo(name = "added_at") val addedAt: Long = 0,
    @ColumnInfo(name = "last_opened_at") val lastOpenedAt: Long? = null,
    @ColumnInfo(name = "metadata_locked") val metadataLocked: Boolean = false,
)
