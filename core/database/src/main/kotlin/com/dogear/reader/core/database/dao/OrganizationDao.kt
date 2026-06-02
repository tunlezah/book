package com.dogear.reader.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dogear.reader.core.database.entity.BookCollectionCrossRef
import com.dogear.reader.core.database.entity.BookTagCrossRef
import com.dogear.reader.core.database.entity.CollectionEntity
import com.dogear.reader.core.database.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrganizationDao {

    @Query("SELECT * FROM collections ORDER BY sort_order, name COLLATE NOCASE")
    fun observeCollections(): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE")
    fun observeTags(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCollection(collection: CollectionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addToCollection(ref: BookCollectionCrossRef)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTag(ref: BookTagCrossRef)

    @Query("DELETE FROM book_collections WHERE book_id = :bookId AND collection_id = :collectionId")
    suspend fun removeFromCollection(bookId: Long, collectionId: Long)

    @Query("DELETE FROM book_tags WHERE book_id = :bookId AND tag_id = :tagId")
    suspend fun removeTag(bookId: Long, tagId: Long)
}
