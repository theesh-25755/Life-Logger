package com.example.lifeloggerapp.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media",
    foreignKeys = [ForeignKey(
        entity = EntryEntity::class,
        parentColumns = ["id"],
        childColumns = ["entryId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("entryId")]
)
data class MediaEntity(
    @PrimaryKey
    val id: String,
    val entryId: String,
    val type: String,
    val storagePath: String,
    val publicUrl: String? = null,
    val durationSec: Int? = null,
    val createdAt: String? = null
)