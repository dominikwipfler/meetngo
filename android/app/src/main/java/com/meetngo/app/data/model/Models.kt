package com.meetngo.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Repräsentiert den eingeloggten Benutzer, wie er vom Backend nach
 * Login/Registrierung oder beim Abruf des eigenen Profils zurückgegeben wird.
 */
data class AuthUser(
    val id: Int,
    val username: String,
    val email: String,
    /** Liste der vom Benutzer ausgewählten Interessen/Kategorien (für personalisierte Vorschläge). */
    val interests: List<String> = emptyList(),
)

/** Antwort des Backends auf Login/Registrierung: enthält das JWT sowie die Benutzerdaten. */
data class AuthResponse(
    val token: String,
    val user: AuthUser,
)

/** Request-Body für den Login-Endpunkt. */
data class LoginRequest(val email: String, val password: String)

/** Request-Body für den Registrierungs-Endpunkt. */
data class RegisterRequest(val username: String, val email: String, val password: String)

/**
 * Repräsentiert eine Veranstaltung, wie sie vom Backend geliefert wird.
 * Die mit [SerializedName] markierten Felder mappen snake_case-JSON-Keys
 * auf idiomatische camelCase-Kotlin-Properties.
 */
data class Event(
    val id: Int,
    val name: String,
    val description: String?,
    val category: String,
    val location: String,
    val lat: Double,
    val lng: Double,
    /** Datum/Zeit der Veranstaltung als ISO-String (Formatierung erfolgt über [com.meetngo.app.util]-Hilfsfunktionen). */
    val date: String,
    /** Bereits formatierter Preis-Anzeigetext (z. B. "Kostenlos" oder "12,50 €"). */
    val price: String,
    /** Numerischer Preiswert, z. B. für Sortierung/Filterung. */
    @SerializedName("price_value") val priceValue: Double,
    /** Maximale Teilnehmerzahl, null bedeutet unbegrenzt. */
    val capacity: Int?,
    /** Aktuelle Anzahl der Teilnehmer/verkauften Tickets. */
    val attendees: Int,
    @SerializedName("image_path") val imagePath: String?,
    @SerializedName("organizer_id") val organizerId: Int?,
    /** Anzeigename des Veranstalters. */
    val organizer: String?,
    /** 1 = als "Featured" hervorgehoben, 0 = normal (Backend liefert Int statt Boolean). */
    val featured: Int = 0,
    /** 1 = aktiv/sichtbar, 0 = vom Veranstalter deaktiviert. */
    val active: Int = 1,
)

/**
 * Repräsentiert ein Ticket eines Benutzers für eine Veranstaltung.
 * Enthält denormalisierte Event-Daten (event_name, event_date, ...), damit
 * die Tickets-Liste ohne zusätzliche Requests pro Event angezeigt werden kann.
 */
data class Ticket(
    val id: Int,
    @SerializedName("event_id") val eventId: Int,
    @SerializedName("user_id") val userId: Int,
    /** Status des Tickets: "active" (gültig) oder "used" (eingelöst/eingecheckt). */
    val status: String,
    @SerializedName("event_name") val eventName: String,
    @SerializedName("event_date") val eventDate: String,
    @SerializedName("event_location") val eventLocation: String,
    @SerializedName("event_image_path") val eventImagePath: String?,
    @SerializedName("event_price") val eventPrice: String,
)

/** Request-Body zum Erstellen/Kaufen eines Tickets für eine bestimmte Veranstaltung. */
data class CreateTicketRequest(val eventId: Int)

/** Request-Body zum Aktualisieren der Featured-/Aktiv-Flags einer Veranstaltung durch den Veranstalter. */
data class UpdateEventRequest(val featured: Boolean? = null, val active: Boolean? = null)

/**
 * Request-Body für Profil-Updates. Alle Felder sind optional (null),
 * damit nur die tatsächlich geänderten Felder ans Backend gesendet werden.
 */
data class UpdateProfileRequest(
    val username: String? = null,
    val email: String? = null,
    val interests: List<String>? = null,
)

/** Gibt an, ob der aktuelle Benutzer einem Veranstalter folgt und wie viele Follower dieser hat. */
data class FollowStatus(val following: Boolean, val followers: Int)

/** Gibt an, ob die aktuelle Veranstaltung in den Favoriten des Benutzers liegt. */
data class FavoriteStatus(val favorited: Boolean)

/** Request-Body zum Ändern des Passworts (aktuelles + neues Passwort). */
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

/** Antwort des Backends auf das Einlösen (Check-in) eines Tickets per QR-Scan. */
data class CheckinResponse(
    val success: Boolean,
    val ticketId: Int,
    val eventName: String,
    val status: String,
)

/** Generische Fehlerstruktur, in die Backend-Fehlerantworten (z. B. 4xx) deserialisiert werden. */
data class ApiError(val error: String)
