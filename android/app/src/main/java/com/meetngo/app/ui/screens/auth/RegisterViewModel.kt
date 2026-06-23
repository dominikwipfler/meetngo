package com.meetngo.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meetngo.app.data.api.ApiService
import com.meetngo.app.data.model.RegisterRequest
import com.meetngo.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI-Zustand des Registrierungs-Screens. */
data class RegisterUiState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val passwordConfirm: String = "",
    val error: String = "",
    val loading: Boolean = false,
    val registered: Boolean = false,
)

/**
 * Hält den UI-Zustand des Registrierungs-Screens und kapselt Validierung sowie
 * den Registrierungs-Aufruf gegen [apiService]/[authRepository].
 */
class RegisterViewModel(
    private val authRepository: AuthRepository,
    private val apiService: ApiService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onUsernameChange(value: String) {
        _uiState.update { it.copy(username = value) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value) }
    }

    fun onPasswordConfirmChange(value: String) {
        _uiState.update { it.copy(passwordConfirm = value) }
    }

    /** Validiert alle Formularfelder lokal, bevor überhaupt ein Netzwerk-Request ausgelöst wird. */
    fun register() {
        val state = _uiState.value
        if (state.username.isBlank() || state.email.isBlank() || state.password.isBlank() || state.passwordConfirm.isBlank()) {
            _uiState.update { it.copy(error = "Alle Felder sind erforderlich") }
            return
        }
        if (state.password != state.passwordConfirm) {
            _uiState.update { it.copy(error = "Passwörter stimmen nicht überein") }
            return
        }
        if (state.password.length < 6) {
            _uiState.update { it.copy(error = "Passwort muss mindestens 6 Zeichen lang sein") }
            return
        }
        _uiState.update { it.copy(error = "", loading = true) }
        viewModelScope.launch {
            try {
                val res = apiService.register(RegisterRequest(state.username, state.email, state.password))
                // Registrierung loggt direkt ein (Backend liefert bereits ein gültiges Token zurück).
                authRepository.login(res.token, res.user)
                _uiState.update { it.copy(loading = false, registered = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(loading = false, error = e.toAuthErrorMessage("Registrierung fehlgeschlagen"))
                }
            }
        }
    }
}
