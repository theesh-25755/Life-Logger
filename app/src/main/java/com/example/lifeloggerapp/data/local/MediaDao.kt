package com.example.lifeloggerapp.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {

    @Query("SELECT * FROM media WHERE entryId = :entryId")
    fun getMediaForEntry(entryId: String): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media WHERE entryId = :entryId")
    suspend fun getMediaForEntryOnce(entryId: String): List<MediaEntity>

    @Upsert
    suspend fun upsertMedia(media: MediaEntity)

    @Upsert
    suspend fun upsertMediaList(mediaList: List<MediaEntity>)

    @Query("DELETE FROM media WHERE id = :id")
    suspend fun deleteMedia(id: String)

    @Query("DELETE FROM media WHERE entryId = :entryId")
    suspend fun deleteMediaForEntry(entryId: String)
}