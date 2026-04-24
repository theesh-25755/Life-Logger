package com.example.lifeloggerapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.lifeloggerapp.data.local.EntryEntity
import com.example.lifeloggerapp.data.local.TagEntity
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.util.UUID
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from

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
                pullFromSupabase()
                _syncState.value = SyncState.Idle
            } catch (e: Exception) {
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
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        val lastSynced = getLastSyncedAt()

        val response = if (lastSynced != null) {
            supabase.postgrest["entries"].select {
                filter {
                    eq("user_id", userId)
                    gt("updated_at", lastSynced)
                }
            }
        } else {
            supabase.postgrest["entries"].select {
                filter { eq("user_id", userId) }
            }
        }

        val remoteEntries = response.decodeList<EntryEntity>()

        remoteEntries.forEach { remote ->
            val local = entryDao.getEntryById(remote.id)
            if (local == null) {
                // New entry from another device
                entryDao.upsertEntry(remote.copy(synced = true))
            } else {
                // Conflict resolution — last write wins by updated_at
                val remoteTime = remote.updatedAt ?: ""
                val localTime = local.updatedAt ?: ""
                if (remoteTime > localTime) {
                    entryDao.upsertEntry(remote.copy(synced = true))
                }
            }
        }

        saveLastSyncedAt(java.time.Instant.now().toString())
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
}