package com.meetngo.app.ui.screens.auth

import com.google.gson.Gson
import com.meetngo.app.data.model.ApiError
import retrofit2.HttpException

/**
 * The backend always responds with { "error": "<German message>" } on failure
 * (see backend/routes/auth.js). Retrofit surfaces non-2xx responses as
 * HttpException, so we pull the message out of the error body when present
 * and fall back to a generic message otherwise.
 */
fun Throwable.toAuthErrorMessage(fallback: String): String {
    if (this is HttpException) {
        // errorBody() kann nur einmal gelesen werden, daher direkt zwischenspeichern.
        val body = response()?.errorBody()?.string()
        parseErrorMessage(body)?.let { return it }
    }
    return fallback
}

/** Extrahiert die "error"-Nachricht aus einem JSON-Fehlerbody, oder null falls nicht vorhanden/valide. */
internal fun parseErrorMessage(body: String?): String? {
    if (body.isNullOrBlank()) return null
    // Fehlerantwort ist nicht immer valides JSON (z. B. bei Netzwerk-/Proxy-Fehlern) – im Zweifel null liefern.
    val parsed = runCatching { Gson().fromJson(body, ApiError::class.java) }.getOrNull()
    return parsed?.error?.takeIf { it.isNotBlank() }
}
