package com.example.lifeloggerapp.entry

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifeloggerapp.data.local.MediaEntity
import com.example.lifeloggerapp.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class MediaState {
    object Idle : MediaState()
    object Uploading : MediaState()
    object Success : MediaState()
    data class Error(val message: String) : MediaState()
}

class MediaViewModel(private val context: Context) : ViewModel() {

    private val repository = MediaRepository(context)

    private val _mediaState = MutableStateFlow<MediaState>(MediaState.Idle)
    val mediaState: StateFlow<MediaState> = _mediaState

    private val _entryMedia = MutableStateFlow<List<MediaEntity>>(emptyList())
    val entryMedia: StateFlow<List<MediaEntity>> = _entryMedia

    fun loadMediaForEntry(entryId: String) {
        viewModelScope.launch {
            repository.getMediaForEntry(entryId).collect {
                _entryMedia.value = it
            }
        }
    }

    fun uploadImage(entryId: String, userId: String, uri: Uri) {
        viewModelScope.launch {
            _mediaState.value = MediaState.Uploading
            val result = repository.uploadImage(entryId, userId, uri)
            _mediaState.value = if (result.isSuccess) MediaState.Success
            else MediaState.Error(result.exceptionOrNull()?.message ?: "Image upload failed")
        }
    }

    fun uploadAudio(entryId: String, userId: String, filePath: String, durationSec: Int) {
        viewModelScope.launch {
            _mediaState.value = MediaState.Uploading
            val result = repository.uploadAudio(entryId, userId, filePath, durationSec)
            _mediaState.value = if (result.isSuccess) MediaState.Success
            else MediaState.Error(result.exceptionOrNull()?.message ?: "Audio upload failed")
        }
    }

    // Uploads all new media in one coroutine — used in edit mode
    fun uploadAllMedia(
        entryId: String,
        userId: String,
        newImageUris: List<Uri>,
        newAudioEntries: List<com.example.lifeloggerapp.ui.screens.AudioEntry>,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            _mediaState.value = MediaState.Uploading
            try {
                newImageUris.forEach { uri -> repository.uploadImage(entryId, userId, uri) }
                newAudioEntries.forEach { audio -> repository.uploadAudio(entryId, userId, audio.filePath, audio.durationSec) }
                _mediaState.value = MediaState.Success  // ← this triggers LaunchedEffect(mediaState) → onBackClick()
            } catch (e: Exception) {
                _mediaState.value = MediaState.Error(e.message ?: "Upload failed")
            }
            onDone()  // reset entry state
        }
    }

    fun deleteMedia(mediaId: String, storagePath: String, type: String) {
        viewModelScope.launch {
            repository.deleteMedia(mediaId, storagePath, type)
        }
    }

    fun resetState() {
        _mediaState.value = MediaState.Idle
    }
}