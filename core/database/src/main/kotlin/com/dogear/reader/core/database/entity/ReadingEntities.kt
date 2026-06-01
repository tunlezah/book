package com.dogear.reader.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * 1:1 reading position. Written atomically and frequently so it survives crashes, power loss,
 * and force-kill (see Performance + Architecture). `progression` is always set as a fallback.
 */
@Entity(
    tableName = "reading_progress",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ReadingProgressEntity(
    @PrimaryKey @ColumnInfo(name = "book_id") val bookId: Long,
    @ColumnInfo(name = "spine_index") val spineIndex: Int? = null,
    val selector: String? = null,
    @ColumnInfo(name = "char_offset") val charOffset: Int? = null,
    @ColumnInfo(name = "text_snippet") val textSnippet: String? = null,
    @ColumnInfo(name = "page_index") val pageIndex: Int? = null,
    val progression: Float = 0f,
    @ColumnInfo(name = "chapter_title") val chapterTitle: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = 0,
)

/** 1:1 reading statistics, accumulated per session. */
@Entity(
    tableName = "reading_stats",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ReadingStatsEntity(
    @PrimaryKey @ColumnInfo(name = "book_id") val bookId: Long,
    @ColumnInfo(name = "total_reading_ms") val totalReadingMs: Long = 0,
    @ColumnInfo(name = "session_count") val sessionCount: Int = 0,
    @ColumnInfo(name = "pages_turned") val pagesTurned: Long = 0,
    @ColumnInfo(name = "started_at") val startedAt: Long? = null,
    @ColumnInfo(name = "finished_at") val finishedAt: Long? = null,
)
