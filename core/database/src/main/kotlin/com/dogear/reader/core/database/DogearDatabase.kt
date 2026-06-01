package com.dogear.reader.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dogear.reader.core.database.dao.BookDao
import com.dogear.reader.core.database.dao.OrganizationDao
import com.dogear.reader.core.database.dao.ReadingProgressDao
import com.dogear.reader.core.database.entity.BookCollectionCrossRef
import com.dogear.reader.core.database.entity.BookEntity
import com.dogear.reader.core.database.entity.BookFileEntity
import com.dogear.reader.core.database.entity.BookFtsEntity
import com.dogear.reader.core.database.entity.BookTagCrossRef
import com.dogear.reader.core.database.entity.BookmarkEntity
import com.dogear.reader.core.database.entity.CollectionEntity
import com.dogear.reader.core.database.entity.HighlightEntity
import com.dogear.reader.core.database.entity.NoteEntity
import com.dogear.reader.core.database.entity.ReadingProgressEntity
import com.dogear.reader.core.database.entity.ReadingStatsEntity
import com.dogear.reader.core.database.entity.TagEntity

/**
 * The single Room database. Schemas are exported (see `schemas/`) and every version bump ships
 * an explicit migration with a test — release builds never fall back to destructive migration,
 * because a personal library must not be silently wiped (see Database Schema doc §5).
 */
@Database(
    entities = [
        BookEntity::class,
        BookFtsEntity::class,
        BookFileEntity::class,
        ReadingProgressEntity::class,
        ReadingStatsEntity::class,
        BookmarkEntity::class,
        HighlightEntity::class,
        NoteEntity::class,
        CollectionEntity::class,
        TagEntity::class,
        BookCollectionCrossRef::class,
        BookTagCrossRef::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class DogearDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun organizationDao(): OrganizationDao

    companion object {
        const val NAME = "dogear.db"
    }
}
