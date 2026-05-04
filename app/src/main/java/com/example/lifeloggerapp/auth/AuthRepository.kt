package com.example.lifeloggerapp.auth

import com.example.lifeloggerapp.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import android.content.Context
import android.net.Uri
import io.github.jan.supabase.storage.storage

@kotlinx.serialization.Serializable
data class ProfileData(
    val id: String = "",
    @kotlinx.serialization.SerialName("display_name") val displayName: String? = null,
    @kotlinx.serialization.SerialName("avatar_url") val avatarUrl: String? = null
)

class AuthRepository {

    suspend fun signUp(email: String, password: String): Result<Unit> {
        return try {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            supabase.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUser() = supabase.auth.currentUserOrNull()

    fun getCurrentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    suspend fun createProfile(userId: String, email: String): Result<Unit> {
        return try {
            val payload = mapOf(
                "id"           to userId,
                "display_name" to email.substringBefore("@")
            )
            supabase.postgrest["profiles"].upsert(payload)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateDisplayName(userId: String, name: String): Result<Unit> {
        return try {
            supabase.postgrest["profiles"].update(
                mapOf("display_name" to name)
            ) {
                filter { eq("id", userId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadProfileImage(userId: String, uri: Uri, context: Context): Result<String> {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                ?: return Result.failure(Exception("Could not read image"))
            val path = "$userId/avatar.jpg"
            supabase.storage["avatars"].upload(path, bytes) {
                upsert = true
            }
            val url = supabase.storage["avatars"].publicUrl(path)
            
            // Append timestamp to URL to bypass Coil caching
            val timestampedUrl = "$url?t=${System.currentTimeMillis()}"
            
            supabase.postgrest["profiles"].update(
                mapOf("avatar_url" to timestampedUrl)
            ) {
                filter { eq("id", userId) }
            }
            Result.success(timestampedUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProfile(userId: String): Result<ProfileData> {
        return try {
            val result = supabase.postgrest["profiles"].select {
                filter { eq("id", userId) }
            }.decodeSingle<ProfileData>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}