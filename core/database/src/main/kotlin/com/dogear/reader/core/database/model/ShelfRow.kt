package com.dogear.reader.core.database.model

import androidx.room.ColumnInfo

/**
 * Narrow projection returned by the shelf paging query — only the columns the grid/list draw,
 * plus reading progress joined from `reading_progress`. Keeping this lean is central to the
 * low-memory, fast-scroll goals.
 */
data class ShelfRow(
    val id: Long,
    val title: String,
    val author: String,
    @ColumnInfo(name = "cover_path") val coverPath: String?,
    @ColumnInfo(name = "cover_is_generated") val coverIsGenerated: Boolean,
    val format: String,
    @ColumnInfo(name = "reading_state") val readingState: String,
    val progress: Float,
)
