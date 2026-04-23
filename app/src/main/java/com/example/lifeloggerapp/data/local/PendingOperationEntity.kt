package com.example.lifeloggerapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class OperationType { CREATE, UPDATE, DELETE }

@Entity(tableName = "pending_operations")
data class PendingOperationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val entryId: String,
    val operationType: String, // OperationType.name
    val createdAt: Long = System.currentTimeMillis()
)