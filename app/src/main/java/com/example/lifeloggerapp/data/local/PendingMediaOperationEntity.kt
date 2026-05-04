package com.example.lifeloggerapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_media_operations")
data class PendingMediaOperationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val entryId: String,
    val mediaId: String,
    val localPath: String,       // file path on device
    val type: String,            // "image" or "audio"
    val durationSec: Int? = null,
    val userId: String,
    val createdAt: Long = System.currentTimeMillis()
)