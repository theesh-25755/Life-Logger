package com.example.lifeloggerapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "entries")
data class EntryEntity(
    @PrimaryKey
    val id: String,
    @SerialName("user_id")    val userId: String,
    val title: String,
    val body: String? = null,
    @SerialName("pull_quote") val pullQuote: String? = null,
    val mood: String? = null,
    val category: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null,
    val synced: Boolean = false
)