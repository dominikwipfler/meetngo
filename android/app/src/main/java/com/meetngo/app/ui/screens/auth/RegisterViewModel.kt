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

/** Identifiziert das Feld, auf das sich [RegisterUiState.error] bezieht (für Feld-Markierung + Fokus). */
enum class RegisterField { USERNAME, EMAIL, PASSWORD, PASSWORD_CONFIRM }

/** UI-Zustand des Registrierungs-Screens. */
data class RegisterUiState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val passwordConfirm: String = "",
    val error: String = "",
    val errorField: RegisterField? = null,
    val loading: Boolean = false,
    val registered: Boolean = false,
)

private const val USERNAME_TAKEN_MESSAGE = "Benutzername bereits vergeben"
private const val EMAIL_TAKEN_MESSAGE = "E-Mail-Adresse bereits registriert"

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
        when {
            state.username.isBlank() -> {
                _uiState.update { it.copy(error = "Alle Felder sind erforderlich", errorField = RegisterField.USERNAME) }
                return
            }
            state.email.isBlank() -> {
                _uiState.update { it.copy(error = "Alle Felder sind erforderlich", errorField = RegisterField.EMAIL) }
                return
            }
            state.password.isBlank() -> {
                _uiState.update { it.copy(error = "Alle Felder sind erforderlich", errorField = RegisterField.PASSWORD) }
                return
            }
            state.passwordConfirm.isBlank() -> {
                _uiState.update { it.copy(error = "Alle Felder sind erforderlich", errorField = RegisterField.PASSWORD_CONFIRM) }
                return
            }
        }
        if (state.password != state.passwordConfirm) {
            _uiState.update { it.copy(error = "Passwörter stimmen nicht überein", errorField = RegisterField.PASSWORD_CONFIRM) }
            return
        }
        if (state.password.length < 6) {
            _uiState.update { it.copy(error = "Passwort muss mindestens 6 Zeichen lang sein", errorField = RegisterField.PASSWORD) }
            return
        }
        _uiState.update { it.copy(error = "", errorField = null, loading = true) }
        viewModelScope.launch {
            try {
                val res = apiService.register(RegisterRequest(state.username, state.email, state.password))
                // Registrierung loggt direkt ein (Backend liefert bereits ein gültiges Token zurück).
                authRepository.login(res.token, res.user)
                _uiState.update { it.copy(loading = false, registered = true) }
            } catch (e: Exception) {
                val message = e.toAuthErrorMessage("Registrierung fehlgeschlagen")
                // Konflikt-Nachrichten kommen wortgleich vom Backend (siehe backend/utils/dbErrors.js)
                // und markieren so direkt das betroffene Feld statt nur die generische Meldung zu zeigen.
                val field = when (message) {
                    USERNAME_TAKEN_MESSAGE -> RegisterField.USERNAME
                    EMAIL_TAKEN_MESSAGE -> RegisterField.EMAIL
                    else -> null
                }
                _uiState.update { it.copy(loading = false, error = message, errorField = field) }
            }
        }
    }
}
