package com.example.lifeloggerapp.data.repository

import android.content.Context
import android.net.Uri
import com.example.lifeloggerapp.database
import com.example.lifeloggerapp.data.local.MediaEntity
import com.example.lifeloggerapp.database
import com.example.lifeloggerapp.data.local.PendingMediaOperationEntity
import com.example.lifeloggerapp.supabase
import io.github.jan.supabase.storage.storage
import java.util.UUID

class MediaRepository(private val context: Context) {

    private val mediaDao = database.mediaDao()

    suspend fun uploadImage(entryId: String, userId: String, uri: Uri): Result<MediaEntity> {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                ?: return Result.failure(Exception("Could not read image"))

            // Save to cache file for potential offline retry
            val cacheFile = java.io.File(context.cacheDir, "pending_img_${System.currentTimeMillis()}.jpg")
            cacheFile.writeBytes(bytes)

            val filename = "img_${System.currentTimeMillis()}.jpg"
            val path = "$userId/$entryId/$filename"

            val media = MediaEntity(
                id = UUID.randomUUID().toString(),
                entryId = entryId,
                type = "image",
                storagePath = path,
                publicUrl = null
            )
            mediaDao.upsertMedia(media)

            try {
                supabase.storage["entry-images"].upload(path, bytes)
                val publicUrl = supabase.storage["entry-images"].publicUrl(path)
                val synced = media.copy(publicUrl = publicUrl)
                mediaDao.upsertMedia(synced)
                cacheFile.delete()
                Result.success(synced)
            } catch (e: Exception) {
                // Queue for retry when online
                database.pendingMediaOperationDao().insert(
                    PendingMediaOperationEntity(
                        entryId = entryId,
                        mediaId = media.id,
                        localPath = cacheFile.absolutePath,
                        type = "image",
                        userId = userId
                    )
                )
                Result.success(media) // return local version, will sync later
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadAudio(entryId: String, userId: String, filePath: String, durationSec: Int): Result<MediaEntity> {
        return try {
            val bytes = java.io.File(filePath).readBytes()
            val filename = "audio_${System.currentTimeMillis()}.m4a"
            val path = "$userId/$entryId/$filename"

            val media = MediaEntity(
                id = UUID.randomUUID().toString(),
                entryId = entryId,
                type = "audio",
                storagePath = path,
                publicUrl = null,
                durationSec = durationSec
            )
            mediaDao.upsertMedia(media)

            try {
                supabase.storage["entry-audio"].upload(path, bytes)
                val publicUrl = supabase.storage["entry-audio"].publicUrl(path)
                val synced = media.copy(publicUrl = publicUrl)
                mediaDao.upsertMedia(synced)
                Result.success(synced)
            } catch (e: Exception) {
                database.pendingMediaOperationDao().insert(
                    PendingMediaOperationEntity(
                        entryId = entryId,
                        mediaId = media.id,
                        localPath = filePath,
                        type = "audio",
                        durationSec = durationSec,
                        userId = userId
                    )
                )
                Result.success(media)
            }
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

    suspend fun queuePendingUpload(
        entryId: String,
        userId: String,
        localPath: String,
        type: String,
        durationSec: Int? = null,
        mediaId: String
    ) {
        database.pendingMediaOperationDao().insert(
            PendingMediaOperationEntity(
                entryId = entryId,
                mediaId = mediaId,
                localPath = localPath,
                type = type,
                durationSec = durationSec,
                userId = userId
            )
        )
    }
}