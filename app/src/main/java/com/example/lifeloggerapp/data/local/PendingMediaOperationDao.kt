package com.example.lifeloggerapp.data.local

import androidx.room.*

@Dao
interface PendingMediaOperationDao {

    @Query("SELECT * FROM pending_media_operations ORDER BY createdAt ASC")
    suspend fun getAllPending(): List<PendingMediaOperationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(op: PendingMediaOperationEntity)

    @Query("DELETE FROM pending_media_operations WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM pending_media_operations WHERE entryId = :entryId")
    suspend fun deleteForEntry(entryId: String)
}