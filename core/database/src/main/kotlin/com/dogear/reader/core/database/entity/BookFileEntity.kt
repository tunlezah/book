package com.dogear.reader.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The on-disk location of a book, kept separate from [BookEntity] so storage moves don't
 * churn metadata rows. `uri` is a SAF/content URI (no broad storage permission required).
 */
@Entity(
    tableName = "book_files",
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
data class BookFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "book_id") val bookId: Long,
    val uri: String,
    @ColumnInfo(name = "display_name") val displayName: String? = null,
    val mime: String? = null,
    @ColumnInfo(name = "content_hash") val contentHash: String,
    @ColumnInfo(name = "is_available") val isAvailable: Boolean = true,
)
