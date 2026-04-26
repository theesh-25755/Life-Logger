package com.example.lifeloggerapp.data.repository

import android.content.Context
import android.net.Uri
import com.example.lifeloggerapp.database
import com.example.lifeloggerapp.data.local.MediaEntity
import com.example.lifeloggerapp.supabase
import io.github.jan.supabase.storage.storage
import java.util.UUID

class MediaRepository(private val context: Context) {

    private val mediaDao = database.mediaDao()

    suspend fun uploadImage(entryId: String, userId: String, uri: Uri): Result<MediaEntity> {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                ?: return Result.failure(Exception("Could not read image"))

            val filename = "img_${System.currentTimeMillis()}.jpg"
            val path = "$userId/$entryId/$filename"

            supabase.storage["entry-images"].upload(path, bytes)

            val publicUrl = supabase.storage["entry-images"].publicUrl(path)

            val media = MediaEntity(
                id = UUID.randomUUID().toString(),
                entryId = entryId,
                type = "image",
                storagePath = path,
                publicUrl = publicUrl
            )

            mediaDao.upsertMedia(media)
            Result.success(media)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadAudio(entryId: String, userId: String, filePath: String, durationSec: Int): Result<MediaEntity> {
        return try {
            val file = java.io.File(filePath)
            val bytes = file.readBytes()

            val filename = "audio_${System.currentTimeMillis()}.m4a"
            val path = "$userId/$entryId/$filename"

            supabase.storage["entry-audio"].upload(path, bytes)

            val publicUrl = supabase.storage["entry-audio"].publicUrl(path)

            val media = MediaEntity(
                id = UUID.randomUUID().toString(),
                entryId = entryId,
                type = "audio",
                storagePath = path,
                publicUrl = publicUrl,
                durationSec = durationSec
            )

            mediaDao.upsertMedia(media)
            Result.success(media)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMedia(mediaId: String, storagePath: String, type: String) {
        try {
            val bucket = if (type == "image") "entry-images" else "entry-audio"
            supabase.storage[bucket].delete(listOf(storagePath))
            mediaDao.deleteMedia(mediaId)
        } catch (e: Exception) {
            android.util.Log.e("MediaRepository", "Delete failed: ${e.message}")
        }
    }

    fun getMediaForEntry(entryId: String) = mediaDao.getMediaForEntry(entryId)
}