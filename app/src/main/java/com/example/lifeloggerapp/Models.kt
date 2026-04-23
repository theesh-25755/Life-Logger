package com.example.lifeloggerapp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Mood {
    @SerialName("sad")      SAD,
    @SerialName("neutral")  NEUTRAL,
    @SerialName("calm")     CALM,
    @SerialName("happy")    HAPPY,
    @SerialName("ecstatic") ECSTATIC
}

@Serializable
enum class MediaType {
    @SerialName("image") IMAGE,
    @SerialName("audio") AUDIO
}

@Serializable
data class Profile(
    val id: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url")   val avatarUrl: String? = null,
    @SerialName("created_at")   val createdAt: String? = null
)

@Serializable
data class Entry(
    val id: String = "",
    @SerialName("user_id")    val userId: String = "",
    val title: String = "",
    val body: String? = null,
    @SerialName("pull_quote") val pullQuote: String? = null,
    val mood: Mood? = null,
    val category: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null,
    val synced: Boolean = false
)

@Serializable
data class Tag(
    val id: String = "",
    @SerialName("entry_id") val entryId: String = "",
    val label: String = "",
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class Media(
    val id: String = "",
    @SerialName("entry_id")     val entryId: String = "",
    val type: MediaType,
    @SerialName("storage_path") val storagePath: String = "",
    @SerialName("public_url")   val publicUrl: String? = null,
    @SerialName("duration_sec") val durationSec: Int? = null,
    @SerialName("created_at")   val createdAt: String? = null
)