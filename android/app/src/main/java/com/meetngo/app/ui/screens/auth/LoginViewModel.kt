package com.meetngo.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meetngo.app.data.api.ApiService
import com.meetngo.app.data.model.LoginRequest
import com.meetngo.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI-Zustand des Login-Screens. */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val error: String = "",
    val loading: Boolean = false,
    val loggedIn: Boolean = false,
)

/**
 * Hält den UI-Zustand des Login-Screens und kapselt den Login-Aufruf gegen
 * [apiService]/[authRepository], statt dass der Composable selbst Netzwerk-
 * und Persistenz-Logik ausführt.
 */
class LoginViewModel(
    private val authRepository: AuthRepository,
    private val apiService: ApiService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value) }
    }

    /** Validiert die Eingaben, ruft den Login-Endpunkt auf und persistiert die Sitzung bei Erfolg. */
    fun login() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(error = "Bitte E-Mail und Passwort eingeben") }
            return
        }
        _uiState.update { it.copy(error = "", loading = true) }
        viewModelScope.launch {
            try {
                val res = apiService.login(LoginRequest(state.email, state.password))
                authRepository.login(res.token, res.user)
                _uiState.update { it.copy(loading = false, loggedIn = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(loading = false, error = e.toAuthErrorMessage("Anmeldung fehlgeschlagen"))
                }
            }
        }
    }
}
