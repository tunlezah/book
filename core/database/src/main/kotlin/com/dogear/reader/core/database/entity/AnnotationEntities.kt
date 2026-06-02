package com.dogear.reader.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["book_id"])],
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "book_id") val bookId: Long,
    val name: String? = null,
    @ColumnInfo(name = "spine_index") val spineIndex: Int? = null,
    val selector: String? = null,
    @ColumnInfo(name = "char_offset") val charOffset: Int? = null,
    @ColumnInfo(name = "text_snippet") val textSnippet: String? = null,
    @ColumnInfo(name = "page_index") val pageIndex: Int? = null,
    val progression: Float = 0f,
    val excerpt: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = 0,
)

@Entity(
    tableName = "highlights",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["book_id"])],
)
data class HighlightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "book_id") val bookId: Long,
    val color: Int,
    @ColumnInfo(name = "selected_text") val selectedText: String,
    @ColumnInfo(name = "start_spine_index") val startSpineIndex: Int? = null,
    @ColumnInfo(name = "start_selector") val startSelector: String? = null,
    @ColumnInfo(name = "start_char_offset") val startCharOffset: Int? = null,
    @ColumnInfo(name = "end_spine_index") val endSpineIndex: Int? = null,
    @ColumnInfo(name = "end_selector") val endSelector: String? = null,
    @ColumnInfo(name = "end_char_offset") val endCharOffset: Int? = null,
    @ColumnInfo(name = "text_snippet") val textSnippet: String? = null,
    val progression: Float = 0f,
    @ColumnInfo(name = "created_at") val createdAt: Long = 0,
)

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = HighlightEntity::class,
            parentColumns = ["id"],
            childColumns = ["highlight_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["book_id"]), Index(value = ["highlight_id"])],
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "book_id") val bookId: Long,
    @ColumnInfo(name = "highlight_id") val highlightId: Long? = null,
    val body: String,
    @ColumnInfo(name = "spine_index") val spineIndex: Int? = null,
    val selector: String? = null,
    @ColumnInfo(name = "char_offset") val charOffset: Int? = null,
    val progression: Float? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = 0,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = 0,
)
