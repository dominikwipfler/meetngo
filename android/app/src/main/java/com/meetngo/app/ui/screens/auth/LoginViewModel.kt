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

/** Identifiziert das Feld, auf das sich [LoginUiState.error] bezieht (für Feld-Markierung + Fokus). */
enum class LoginField { EMAIL, PASSWORD }

/** UI-Zustand des Login-Screens. */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val error: String = "",
    val errorField: LoginField? = null,
    val loading: Boolean = false,
    val loggedIn: Boolean = false,
)

/**
 * Einmaliger In-Memory-Holder (analog zu [com.meetngo.app.ui.theme.ThemeState]) für die E-Mail,
 * die ein Nutzer bei der Registrierung eingegeben hat, falls er von dort über "Jetzt einloggen"
 * zum Login wechselt (z. B. nach einem E-Mail-bereits-vergeben-Konflikt). Wird beim nächsten
 * Start des Login-Screens einmalig konsumiert, ohne den Nav-Graph mit einem Argument zu erweitern.
 */
object PendingLoginEmail {
    private var email: String? = null

    fun set(value: String) {
        email = value
    }

    /** Liest den vorgemerkten Wert und leert ihn sofort, damit er nicht erneut zugewiesen wird. */
    fun consume(): String? {
        val value = email
        email = null
        return value
    }
}

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

    init {
        PendingLoginEmail.consume()?.let { prefillEmail ->
            _uiState.update { it.copy(email = prefillEmail) }
        }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value) }
    }

    /** Validiert die Eingaben, ruft den Login-Endpunkt auf und persistiert die Sitzung bei Erfolg. */
    fun login() {
        val state = _uiState.value
        if (state.email.isBlank()) {
            _uiState.update { it.copy(error = "Bitte E-Mail und Passwort eingeben", errorField = LoginField.EMAIL) }
            return
        }
        if (state.password.isBlank()) {
            _uiState.update { it.copy(error = "Bitte E-Mail und Passwort eingeben", errorField = LoginField.PASSWORD) }
            return
        }
        _uiState.update { it.copy(error = "", errorField = null, loading = true) }
        viewModelScope.launch {
            try {
                val res = apiService.login(LoginRequest(state.email, state.password))
                authRepository.login(res.token, res.user)
                _uiState.update { it.copy(loading = false, loggedIn = true) }
            } catch (e: Exception) {
                // Bewusst kein errorField hier: bei "Ungültige Anmeldedaten" (401) würde eine
                // Feld-Markierung verraten, ob E-Mail oder Passwort falsch war (Account-Enumeration).
                _uiState.update {
                    it.copy(loading = false, error = e.toAuthErrorMessage("Anmeldung fehlgeschlagen"))
                }
            }
        }
    }
}
