package com.dogear.reader.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.dogear.reader.core.database.entity.ReadingProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingProgressDao {

    /** Atomic upsert called frequently from the reader so position survives a crash/force-kill. */
    @Upsert
    suspend fun upsert(progress: ReadingProgressEntity)

    @Query("SELECT * FROM reading_progress WHERE book_id = :bookId")
    fun observeProgress(bookId: Long): Flow<ReadingProgressEntity?>

    @Query("SELECT * FROM reading_progress WHERE book_id = :bookId")
    suspend fun getProgress(bookId: Long): ReadingProgressEntity?
}
