package com.example.lifeloggerapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.lifeloggerapp.data.local.EntryEntity
import com.example.lifeloggerapp.data.local.MediaEntity
import com.example.lifeloggerapp.data.local.TagEntity
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.util.UUID
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Error(val message: String) : SyncState()
}

class SyncManager(private val context: Context) {

    private val entryDao = database.entryDao()
    private val tagDao = database.tagDao()
    private val pendingDao = database.pendingOperationDao()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun sync() {
        if (!isOnline()) return
        if (_syncState.value is SyncState.Syncing) return

        withContext(Dispatchers.IO) {
            _syncState.value = SyncState.Syncing
            try {
                replayPendingOperations()
                replayPendingMediaUploads()
                pullFromSupabase()
                _syncState.value = SyncState.Idle
            } catch (e: Exception) {
                android.util.Log.e("SyncManager", "Sync failed: ${e.message}", e)
                _syncState.value = SyncState.Error(e.message ?: "Sync failed")
            }
        }
    }

    // ── Push pending local operations to Supabase ─────────────

    private suspend fun replayPendingOperations() {
        val pending = pendingDao.getAllPending()

        pending.forEach { operation ->
            try {
                when (operation.operationType) {
                    "CREATE" -> {
                        val entry = entryDao.getEntryById(operation.entryId) ?: return@forEach
                        pushEntry(entry)
                    }
                    "UPDATE" -> {
                        val entry = entryDao.getEntryById(operation.entryId) ?: return@forEach
                        updateEntryOnSupabase(entry)
                    }
                    "DELETE" -> {
                        softDeleteOnSupabase(operation.entryId)
                    }
                }
                pendingDao.delete(operation.id)
            } catch (e: Exception) {
                // Leave in queue, retry next sync
                android.util.Log.e("SyncManager", "Failed to replay op ${operation.id}: ${e.message}")
            }
        }
    }

    private suspend fun replayPendingMediaUploads() {
        val pending = database.pendingMediaOperationDao().getAllPending()
        pending.forEach { op ->
            try {
                val file = java.io.File(op.localPath)
                if (!file.exists()) {
                    // File no longer on device — remove from queue
                    database.pendingMediaOperationDao().delete(op.id)
                    return@forEach
                }
                val bytes = file.readBytes()
                val bucket = if (op.type == "image") "entry-images" else "entry-audio"
                supabase.storage[bucket].upload(op.localPath.substringAfterLast("/").let {
                    "${op.userId}/${op.entryId}/$it"
                }, bytes)
                val publicUrl = supabase.storage[bucket].publicUrl(
                    "${op.userId}/${op.entryId}/${op.localPath.substringAfterLast("/")}"
                )
                // Update media row with public URL
                val mediaDao = database.mediaDao()
                val existing = mediaDao.getMediaForEntryOnce(op.entryId)
                    .firstOrNull { it.id == op.mediaId }
                if (existing != null) {
                    mediaDao.upsertMedia(existing.copy(publicUrl = publicUrl))
                }
                database.pendingMediaOperationDao().delete(op.id)
            } catch (e: Exception) {
                android.util.Log.e("SyncManager", "Media replay failed: ${e.message}")
            }
        }
    }

    private suspend fun pushEntry(entry: EntryEntity) {
        val payload = mapOf(
            "id"         to entry.id,
            "user_id"    to entry.userId,
            "title"      to entry.title,
            "body"       to entry.body,
            "pull_quote" to entry.pullQuote,
            "mood"       to entry.mood,
            "category"   to entry.category,
            "created_at" to entry.createdAt,
            "updated_at" to entry.updatedAt,
            "deleted_at" to entry.deletedAt
        )
        supabase.postgrest["entries"].insert(payload)

        val tags = tagDao.getTagsForEntryOnce(entry.id)
        tags.forEach { tag ->
            val tagPayload = mapOf(
                "id"       to tag.id,
                "entry_id" to tag.entryId,
                "label"    to tag.label
            )
            supabase.postgrest["tags"].insert(tagPayload)
        }

        entryDao.upsertEntry(entry.copy(synced = true))
    }

    private suspend fun updateEntryOnSupabase(entry: EntryEntity) {
        val payload = mapOf(
            "title"      to entry.title,
            "body"       to entry.body,
            "pull_quote" to entry.pullQuote,
            "mood"       to entry.mood,
            "category"   to entry.category,
            "updated_at" to entry.updatedAt
        )
        supabase.postgrest["entries"].update(payload) {
            filter { eq("id", entry.id) }
        }

        supabase.postgrest["tags"].delete {
            filter { eq("entry_id", entry.id) }
        }

        val tags = tagDao.getTagsForEntryOnce(entry.id)
        tags.forEach { tag ->
            val tagPayload = mapOf(
                "id"       to tag.id,
                "entry_id" to tag.entryId,
                "label"    to tag.label
            )
            supabase.postgrest["tags"].insert(tagPayload)
        }

        entryDao.upsertEntry(entry.copy(synced = true))
    }

    private suspend fun softDeleteOnSupabase(entryId: String) {
        supabase.postgrest["entries"].update(
            mapOf("deleted_at" to java.time.Instant.now().toString())
        ) {
            filter { eq("id", entryId) }
        }
    }

    // ── Pull changes from Supabase since last sync ────────────

    private suspend fun pullFromSupabase() {
        val userId = supabase.auth.currentUserOrNull()?.id ?: run {
            android.util.Log.e("SyncManager", "Pull skipped: no user")
            return
        }
        android.util.Log.d("SyncManager", "Pulling for userId=$userId")

        try {
            // Always pull ALL entries for this user — delta filtering caused missed entries
            val response = supabase.postgrest["entries"].select {
                filter { eq("user_id", userId) }
            }

            val remoteEntries = response.decodeList<EntryEntity>()
            android.util.Log.d("SyncManager", "Fetched ${remoteEntries.size} entries from Supabase")

            remoteEntries.forEach { remote ->
                val local = entryDao.getEntryById(remote.id)
                if (local == null) {
                    entryDao.upsertEntry(remote.copy(synced = true))
                } else {
                    val remoteTime = remote.updatedAt ?: ""
                    val localTime = local.updatedAt ?: ""
                    if (remoteTime > localTime) {
                        entryDao.upsertEntry(remote.copy(synced = true))
                    }
                }
            }

            // Pull ALL media for this user's entries
            val allEntryIds = remoteEntries.map { it.id }
            if (allEntryIds.isNotEmpty()) {
                val mediaResponse = supabase.postgrest["media"].select {
                    filter { isIn("entry_id", allEntryIds) }
                }
                val remoteMedia = mediaResponse.decodeList<MediaEntity>()
                android.util.Log.d("SyncManager", "Fetched ${remoteMedia.size} media items")
                remoteMedia.forEach { database.mediaDao().upsertMedia(it) }
            }

            saveLastSyncedAt(java.time.Instant.now().toString())
        } catch (e: Exception) {
            android.util.Log.e("SyncManager", "pullFromSupabase failed: ${e.message}", e)
            throw e
        }
    }

    // ── Last synced timestamp (SharedPreferences) ─────────────

    private fun getLastSyncedAt(): String? {
        val prefs = context.getSharedPreferences("lifelog_sync", Context.MODE_PRIVATE)
        return prefs.getString("last_synced_at", null)
    }

    private fun saveLastSyncedAt(timestamp: String) {
        val prefs = context.getSharedPreferences("lifelog_sync", Context.MODE_PRIVATE)
        prefs.edit().putString("last_synced_at", timestamp).apply()
    }

    fun clearLastSyncedAt() {
        val prefs = context.getSharedPreferences("lifelog_sync", Context.MODE_PRIVATE)
        prefs.edit().remove("last_synced_at").apply()
    }
}