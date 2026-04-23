package com.example.lifeloggerapp.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags WHERE entryId = :entryId")
    fun getTagsForEntry(entryId: String): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE entryId = :entryId")
    suspend fun getTagsForEntryOnce(entryId: String): List<TagEntity>

    @Upsert
    suspend fun upsertTag(tag: TagEntity)

    @Upsert
    suspend fun upsertTags(tags: List<TagEntity>)

    @Query("DELETE FROM tags WHERE entryId = :entryId")
    suspend fun deleteTagsForEntry(entryId: String)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteTag(id: String)
}