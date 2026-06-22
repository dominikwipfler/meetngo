package com.meetngo.app.data.api

import com.meetngo.app.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

/**
 * Definiert alle REST-Endpunkte des MeetNGo-Backends als Retrofit-Interface.
 * Die Implementierung wird zur Laufzeit von Retrofit generiert (siehe [ApiClient.create]).
 * Alle Funktionen sind `suspend`, da sie auf einem IO-Dispatcher ausgeführt werden.
 */
interface ApiService {
    /** Registriert einen neuen Benutzer und liefert sofort ein gültiges Auth-Token zurück. */
    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    /** Meldet einen Benutzer mit E-Mail/Passwort an. */
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    /**
     * Lädt die Liste der Veranstaltungen, optional gefiltert/sortiert.
     * Alle Query-Parameter sind optional; `null` bedeutet "kein Filter".
     *
     * @param search Freitextsuche über Name/Beschreibung.
     * @param category Filter nach Kategorie (z. B. "Sport", "Musik").
     * @param sort Feld, nach dem sortiert wird (z. B. "date", "price").
     * @param order Sortierrichtung ("asc"/"desc").
     * @param priceFilter Preisfilter, z. B. "free" für kostenlose Events.
     * @param organizerId Liefert nur Events eines bestimmten Veranstalters.
     */
    @GET("api/events")
    suspend fun getEvents(
        @Query("search") search: String? = null,
        @Query("category") category: String? = null,
        @Query("sort") sort: String? = null,
        @Query("order") order: String? = null,
        @Query("priceFilter") priceFilter: String? = null,
        @Query("organizerId") organizerId: Int? = null,
    ): List<Event>

    /** Lädt die Detaildaten einer einzelnen Veranstaltung anhand ihrer ID. */
    @GET("api/events/{id}")
    suspend fun getEvent(@Path("id") id: Int): Event

    /**
     * Erstellt eine neue Veranstaltung als Multipart-Request, damit optional
     * ein Bild ([image]) zusammen mit den restlichen Formularfeldern ([fields])
     * in einem einzigen Request hochgeladen werden kann.
     */
    @Multipart
    @POST("api/events")
    suspend fun createEvent(
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part image: MultipartBody.Part?,
    ): Event

    /** Aktualisiert einzelne Felder (z. B. featured/active) einer Veranstaltung des Veranstalters. */
    @PATCH("api/events/{id}")
    suspend fun updateEvent(@Path("id") id: Int, @Body body: UpdateEventRequest): Event

    /** Löscht eine Veranstaltung (nur durch deren Veranstalter erlaubt). */
    @DELETE("api/events/{id}")
    suspend fun deleteEvent(@Path("id") id: Int)

    /** Lädt alle Veranstaltungen, die der eingeloggte Benutzer als Favorit markiert hat. */
    @GET("api/events/favorites")
    suspend fun getFavorites(): List<Event>

    /** Prüft, ob der eingeloggte Benutzer eine Veranstaltung als Favorit gespeichert hat. */
    @GET("api/events/{id}/favorite-status")
    suspend fun favoriteStatus(@Path("id") id: Int): FavoriteStatus

    /** Markiert eine Veranstaltung als Favorit des eingeloggten Benutzers. */
    @POST("api/events/{id}/favorite")
    suspend fun favoriteEvent(@Path("id") id: Int): FavoriteStatus

    /** Entfernt eine Veranstaltung aus den Favoriten des eingeloggten Benutzers. */
    @DELETE("api/events/{id}/favorite")
    suspend fun unfavoriteEvent(@Path("id") id: Int): FavoriteStatus

    /** Lädt alle Tickets des aktuell eingeloggten Benutzers. */
    @GET("api/tickets")
    suspend fun getMyTickets(): List<Ticket>

    /** Kauft/erstellt ein Ticket für die im Request-Body angegebene Veranstaltung. */
    @POST("api/tickets")
    suspend fun createTicket(@Body body: CreateTicketRequest): Ticket

    /** Storniert/löscht ein Ticket des aktuell eingeloggten Benutzers. */
    @DELETE("api/tickets/{id}")
    suspend fun deleteTicket(@Path("id") id: Int)

    @POST("api/tickets/{id}/checkin")
    suspend fun checkinTicket(@Path("id") id: Int): CheckinResponse

    /** Lädt das Profil des aktuell eingeloggten Benutzers. */
    @GET("api/users/me")
    suspend fun getMyProfile(): AuthUser

    /** Aktualisiert Profilfelder (Username, E-Mail, Interessen) des eingeloggten Benutzers. */
    @PATCH("api/users/me")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): AuthUser

    /** Ändert das Passwort des eingeloggten Benutzers (erfordert i. d. R. das aktuelle Passwort im Body). */
    @PATCH("api/users/me/password")
    suspend fun changePassword(@Body body: ChangePasswordRequest)

    /** Prüft, ob der eingeloggte Benutzer dem Veranstalter mit der gegebenen ID folgt. */
    @GET("api/users/{id}/follow-status")
    suspend fun followStatus(@Path("id") id: Int): FollowStatus

    /** Lässt den eingeloggten Benutzer einem Veranstalter folgen. */
    @POST("api/users/{id}/follow")
    suspend fun followUser(@Path("id") id: Int): FollowStatus

    /** Beendet das Folgen eines Veranstalters durch den eingeloggten Benutzer. */
    @DELETE("api/users/{id}/follow")
    suspend fun unfollowUser(@Path("id") id: Int): FollowStatus
}
