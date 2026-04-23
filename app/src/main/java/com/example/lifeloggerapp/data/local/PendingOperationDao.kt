package com.example.lifeloggerapp.data.local

import androidx.room.*

@Dao
interface PendingOperationDao {

    @Query("SELECT * FROM pending_operations ORDER BY createdAt ASC")
    suspend fun getAllPending(): List<PendingOperationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(operation: PendingOperationEntity)

    @Query("DELETE FROM pending_operations WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM pending_operations WHERE entryId = :entryId")
    suspend fun deleteForEntry(entryId: String)

    @Query("DELETE FROM pending_operations")
    suspend fun clearAll()
}