package com.example.lifeloggerapp.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifeloggerapp.auth.AuthRepository
import com.example.lifeloggerapp.data.local.EntryEntity
import com.example.lifeloggerapp.data.repository.EntryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed class EntryState {
    object Idle : EntryState()
    object Loading : EntryState()
    object Success : EntryState()
    data class Error(val message: String) : EntryState()
}

class EntryViewModel : ViewModel() {

    private val repository = EntryRepository()
    private val authRepository = AuthRepository()

    private val _entries = MutableStateFlow<List<EntryEntity>>(emptyList())
    val entries: StateFlow<List<EntryEntity>> = _entries

    private val _entryState = MutableStateFlow<EntryState>(EntryState.Idle)
    val entryState: StateFlow<EntryState> = _entryState

    private val _searchResults = MutableStateFlow<List<EntryEntity>>(emptyList())
    val searchResults: StateFlow<List<EntryEntity>> = _searchResults

    init {
        loadEntries()
    }

    fun loadEntries() {
        viewModelScope.launch {
            repository.getAllEntries()
                .catch { e ->
                    _entryState.value = EntryState.Error(e.message ?: "Failed to load entries")
                }
                .collect { _entries.value = it }
        }
    }

    fun createEntry(
        title: String,
        body: String?,
        mood: String?,
        category: String?,
        tags: List<String>,
        onCreated: ((String) -> Unit)? = null
    ) {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            _entryState.value = EntryState.Loading
            try {
                val entry = repository.createEntry(userId, title, body, mood, category, tags)
                onCreated?.invoke(entry.id)
                _entryState.value = EntryState.Success
            } catch (e: Exception) {
                _entryState.value = EntryState.Error(e.message ?: "Failed to create entry")
            }
        }
    }

    suspend fun getEntryById(id: String) = repository.getEntryById(id)

    fun updateEntry(
        id: String,
        title: String,
        body: String?,
        mood: String?,
        category: String?,
        tags: List<String>
    ) {
        viewModelScope.launch {
            _entryState.value = EntryState.Loading
            try {
                repository.updateEntry(id, title, body, mood, category, tags)
                _entryState.value = EntryState.Success
            } catch (e: Exception) {
                _entryState.value = EntryState.Error(e.message ?: "Failed to update entry")
            }
        }
    }

    fun deleteEntry(id: String) {
        viewModelScope.launch {
            _entryState.value = EntryState.Loading
            try {
                repository.deleteEntry(id)
                _entryState.value = EntryState.Success
            } catch (e: Exception) {
                _entryState.value = EntryState.Error(e.message ?: "Failed to delete entry")
            }
        }
    }

    fun searchEntries(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _searchResults.value = emptyList()
                return@launch
            }
            repository.searchEntries(query)
                .catch { e ->
                    _entryState.value = EntryState.Error(e.message ?: "Search failed")
                }
                .collect { _searchResults.value = it }
        }
    }

    fun filterByMood(mood: String) {
        viewModelScope.launch {
            repository.getEntriesByMood(mood)
                .catch { e ->
                    _entryState.value = EntryState.Error(e.message ?: "Filter failed")
                }
                .collect { _entries.value = it }
        }
    }

    fun clearFilter() = loadEntries()

    fun resetState() {
        _entryState.value = EntryState.Idle
    }
}