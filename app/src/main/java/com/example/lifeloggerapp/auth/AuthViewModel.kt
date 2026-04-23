package com.example.lifeloggerapp.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun isLoggedIn(): Boolean = repository.getCurrentUser() != null

    fun getCurrentUser() = repository.getCurrentUser()

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.signUp(email, password)
            _authState.value = if (result.isSuccess) {
                AuthState.Success
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Sign up failed")
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.signIn(email, password)
            _authState.value = if (result.isSuccess) {
                AuthState.Success
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Sign in failed")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.signOut()
            _authState.value = if (result.isSuccess) {
                AuthState.Success
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Sign out failed")
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}