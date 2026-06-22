package com.meetngo.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.meetngo.app.data.api.ApiClient
import com.meetngo.app.data.model.AuthUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** DataStore-Instanz, in der Token und Benutzerdaten persistent (über App-Neustarts hinweg) gespeichert werden. */
private val Context.authDataStore by preferencesDataStore(name = "auth")

/**
 * Verwaltet den Authentifizierungszustand der App: persistiert Token und
 * Benutzerdaten in Jetpack DataStore und hält [ApiClient] über dessen
 * In-Memory-Token synchron, damit jeder Request automatisch authentifiziert ist.
 */
class AuthRepository(private val context: Context) {
    private val tokenKey = stringPreferencesKey("token")
    private val userKey = stringPreferencesKey("user")
    private val gson = Gson()

    /** Liefert den aktuell gespeicherten Benutzer als reaktiven Stream (null = nicht eingeloggt oder Parsing fehlgeschlagen). */
    val userFlow: Flow<AuthUser?> = context.authDataStore.data.map { prefs ->
        prefs[userKey]?.let { runCatching { gson.fromJson(it, AuthUser::class.java) }.getOrNull() }
    }

    /** Reaktiver Stream, der angibt, ob aktuell ein Token gespeichert ist (= Benutzer eingeloggt). */
    val isAuthenticatedFlow: Flow<Boolean> = context.authDataStore.data.map { it[tokenKey] != null }

    /** Liest das aktuell gespeicherte Token einmalig (nicht reaktiv) aus dem DataStore. */
    suspend fun getToken(): String? = context.authDataStore.data.first()[tokenKey]

    /** Persistiert Token und Benutzer nach erfolgreichem Login/Registrierung und aktiviert das Token im [ApiClient]. */
    suspend fun login(token: String, user: AuthUser) {
        context.authDataStore.edit {
            it[tokenKey] = token
            it[userKey] = gson.toJson(user)
        }
        ApiClient.setToken(token)
    }

    /** Entfernt Token und Benutzer aus dem DataStore und deaktiviert das Token im [ApiClient]. */
    suspend fun logout() {
        context.authDataStore.edit {
            it.remove(tokenKey)
            it.remove(userKey)
        }
        ApiClient.setToken(null)
    }

    /** Überschreibt die gespeicherten Benutzerdaten, z. B. nachdem das Profil bearbeitet wurde. */
    suspend fun updateUser(user: AuthUser) {
        context.authDataStore.edit { it[userKey] = gson.toJson(user) }
    }

    /** Restores the in-memory token on process start so ApiClient requests are authenticated. */
    suspend fun restoreSession() {
        ApiClient.setToken(getToken())
    }
}
