package com.example.lifeloggerapp.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {

    @Query("SELECT * FROM entries WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun getAllEntries(): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE id = :id AND deletedAt IS NULL")
    suspend fun getEntryById(id: String): EntryEntity?

    @Query("SELECT * FROM entries WHERE userId = :userId AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun getAllEntriesForUser(userId: String): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE updatedAt > :since AND deletedAt IS NULL")
    suspend fun getEntriesModifiedSince(since: String): List<EntryEntity>

    @Query("SELECT * FROM entries WHERE synced = 0 AND deletedAt IS NULL")
    suspend fun getPendingEntries(): List<EntryEntity>

    @Query("""
        SELECT * FROM entries 
        WHERE deletedAt IS NULL 
        AND (title LIKE '%' || :query || '%' OR body LIKE '%' || :query || '%')
        ORDER BY createdAt DESC
    """)
    fun searchEntries(query: String): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE mood = :mood AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun getEntriesByMood(mood: String): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE createdAt LIKE :datePrefix || '%' AND deletedAt IS NULL")
    fun getEntriesByDate(datePrefix: String): Flow<List<EntryEntity>>

    @Upsert
    suspend fun upsertEntry(entry: EntryEntity)

    @Upsert
    suspend fun upsertEntries(entries: List<EntryEntity>)

    @Query("UPDATE entries SET deletedAt = :timestamp, synced = 0 WHERE id = :id")
    suspend fun softDeleteEntry(id: String, timestamp: String)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun hardDeleteEntry(id: String)
}