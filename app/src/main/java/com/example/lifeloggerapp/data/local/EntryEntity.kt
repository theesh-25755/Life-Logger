package com.example.lifeloggerapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entries")
data class EntryEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val title: String,
    val body: String? = null,
    val pullQuote: String? = null,
    val mood: String? = null,
    val category: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val deletedAt: String? = null,
    val synced: Boolean = false
)