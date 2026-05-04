package com.example.lifeloggerapp.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
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
    @SerialName("entry_id")     val entryId: String,
    val type: String,
    @SerialName("storage_path") val storagePath: String,
    @SerialName("public_url")   val publicUrl: String? = null,
    @SerialName("duration_sec") val durationSec: Int? = null,
    @SerialName("created_at")   val createdAt: String? = null
)