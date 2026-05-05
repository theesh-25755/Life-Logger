package com.example.lifeloggerapp.data.repository

import com.example.lifeloggerapp.database
import com.example.lifeloggerapp.data.local.EntryEntity
import com.example.lifeloggerapp.data.local.PendingOperationEntity
import com.example.lifeloggerapp.data.local.TagEntity
import com.example.lifeloggerapp.supabase
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class EntryRepository {

    private val entryDao = database.entryDao()
    private val tagDao = database.tagDao()
    private val pendingDao = database.pendingOperationDao()

    // ── Read ──────────────────────────────────────────────────

    fun getAllEntries(): Flow<List<EntryEntity>> = entryDao.getAllEntries()

    fun searchEntries(query: String): Flow<List<EntryEntity>> = entryDao.searchEntries(query)

    fun getEntriesByMood(mood: String): Flow<List<EntryEntity>> = entryDao.getEntriesByMood(mood)

    suspend fun getEntryById(id: String): EntryEntity? = entryDao.getEntryById(id)

    fun getAllEntriesForUser(userId: String): Flow<List<EntryEntity>> = entryDao.getAllEntriesForUser(userId)

    // ── Create ────────────────────────────────────────────────

    suspend fun createEntry(
        userId: String,
        title: String,
        body: String?,
        mood: String?,
        category: String?,
        tags: List<String>
    ): EntryEntity {
        val now = java.time.Instant.now().toString()
        val entry = EntryEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            title = title,
            body = body,
            mood = mood,
            category = category,
            createdAt = now,
            updatedAt = now,
            synced = false
        )

        // Save locally first
        entryDao.upsertEntry(entry)

        // Save tags locally
        tags.forEach { label ->
            tagDao.upsertTag(
                TagEntity(
                    id = UUID.randomUUID().toString(),
                    entryId = entry.id,
                    label = label
                )
            )
        }

        // Queue for sync
        pendingDao.insert(
            PendingOperationEntity(
                entryId = entry.id,
                operationType = "CREATE"
            )
        )

        // Try to push to Supabase immediately if online
        tryPushToSupabase(entry, tags)

        return entry
    }

    // ── Update ────────────────────────────────────────────────

    suspend fun updateEntry(
        id: String,
        title: String,
        body: String?,
        mood: String?,
        category: String?,
        tags: List<String>
    ) {
        val existing = entryDao.getEntryById(id) ?: return
        val updated = existing.copy(
            title = title,
            body = body,
            mood = mood,
            category = category,
            updatedAt = java.time.Instant.now().toString(),
            synced = false
        )

        entryDao.upsertEntry(updated)

        // Replace tags
        tagDao.deleteTagsForEntry(id)
        tags.forEach { label ->
            tagDao.upsertTag(
                TagEntity(
                    id = UUID.randomUUID().toString(),
                    entryId = id,
                    label = label
                )
            )
        }

        pendingDao.insert(
            PendingOperationEntity(
                entryId = id,
                operationType = "UPDATE"
            )
        )

        tryPushUpdateToSupabase(updated, tags)
    }

    // ── Delete ────────────────────────────────────────────────

    suspend fun deleteEntry(id: String) {
        val timestamp = java.time.Instant.now().toString()
        entryDao.softDeleteEntry(id, timestamp)

        pendingDao.insert(
            PendingOperationEntity(
                entryId = id,
                operationType = "DELETE"
            )
        )

        tryDeleteFromSupabase(id)
    }

    // ── Supabase push helpers ─────────────────────────────────

    private suspend fun tryPushToSupabase(entry: EntryEntity, tags: List<String>) {
        try {
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

            tags.forEach { label ->
                val tagPayload = mapOf(
                    "id"       to UUID.randomUUID().toString(),
                    "entry_id" to entry.id,
                    "label"    to label
                )
                supabase.postgrest["tags"].insert(tagPayload)
            }

            entryDao.upsertEntry(entry.copy(synced = true))
            pendingDao.deleteForEntry(entry.id)
        } catch (e: Exception) {
            android.util.Log.e("SupabaseSync", "Push failed: ${e.message}", e)
        }
    }

    private suspend fun tryPushUpdateToSupabase(entry: EntryEntity, tags: List<String>) {
        try {
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

            tags.forEach { label ->
                val tagPayload = mapOf(
                    "id"       to UUID.randomUUID().toString(),
                    "entry_id" to entry.id,
                    "label"    to label
                )
                supabase.postgrest["tags"].insert(tagPayload)
            }

            entryDao.upsertEntry(entry.copy(synced = true))
            pendingDao.deleteForEntry(entry.id)
        } catch (e: Exception) {
            // Stays in pending queue
        }
    }

    private suspend fun tryDeleteFromSupabase(id: String) {
        try {
            supabase.postgrest["entries"]
                .update({ set("deleted_at", java.time.Instant.now().toString()) }) {
                    filter { eq("id", id) }
                }
            pendingDao.deleteForEntry(id)
        } catch (e: Exception) {
            // Stays in pending queue
        }
    }
}