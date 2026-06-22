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
        if (!body.isNullOrBlank()) {
            // Fehlerantwort ist nicht immer valides JSON (z. B. bei Netzwerk-/Proxy-Fehlern) – im Zweifel auf fallback zurückfallen.
            val parsed = runCatching { Gson().fromJson(body, ApiError::class.java) }.getOrNull()
            if (parsed?.error?.isNotBlank() == true) return parsed.error
        }
    }
    return fallback
}
