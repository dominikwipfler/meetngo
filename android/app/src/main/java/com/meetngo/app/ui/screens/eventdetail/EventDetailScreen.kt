@file:OptIn(ExperimentalMaterial3Api::class)

package com.meetngo.app.ui.screens.eventdetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.meetngo.app.data.api.ApiService
import com.meetngo.app.data.api.BASE_URL
import com.meetngo.app.data.model.ApiError
import com.meetngo.app.data.model.CreateTicketRequest
import com.meetngo.app.data.model.Event
import com.meetngo.app.ui.navigation.Routes
import com.meetngo.app.ui.theme.MeetNGoColors
import com.meetngo.app.util.formatDateLong
import kotlinx.coroutines.launch
import retrofit2.HttpException

/** Extrahiert die deutsche Backend-Fehlermeldung aus einer fehlgeschlagenen Antwort, analog zu AuthErrorUtils.kt. */
private fun Throwable.toApiErrorMessage(fallback: String): String {
    if (this is HttpException) {
        val body = response()?.errorBody()?.string()
        if (!body.isNullOrBlank()) {
            val parsed = runCatching { Gson().fromJson(body, ApiError::class.java) }.getOrNull()
            if (parsed?.error?.isNotBlank() == true) return parsed.error
        }
    }
    return fallback
}

/** Baut aus einem relativen Backend-Pfad eine vollständige Bild-URL; absolute URLs werden unverändert übernommen. */
private fun imageUrl(imagePath: String?): String? {
    if (imagePath.isNullOrBlank()) return null
    if (imagePath.startsWith("http")) return imagePath
    return BASE_URL.removeSuffix("/") + "/" + imagePath.trimStart('/')
}

/**
 * Detailansicht einer Veranstaltung: Bild-Header, Kerndaten, Veranstalter-Info
 * mit Folgen-Button, Beschreibung sowie ein Bottom-Sheet zum Ticketkauf
 * (bzw. zur kostenlosen Teilnahme).
 */
@Composable
fun EventDetailScreen(navController: NavHostController, apiService: ApiService, eventId: Int) {
    var event by remember { mutableStateOf<Event?>(null) }
    var loading by remember { mutableStateOf(true) }
    var showTicketSheet by remember { mutableStateOf(false) }
    var quantity by remember { mutableStateOf(1) }
    var purchasing by remember { mutableStateOf(false) }
    var purchaseError by remember { mutableStateOf("") }
    var following by remember { mutableStateOf(false) }
    var favorited by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Lädt die Eventdaten neu, sobald sich die übergebene eventId ändert.
    LaunchedEffect(eventId) {
        loading = true
        try {
            event = apiService.getEvent(eventId)
        } catch (e: Exception) {
            event = null
        } finally {
            loading = false
        }
    }

    // Sobald die Veranstalter-ID bekannt ist (nach dem Laden des Events), Folgen-Status separat abfragen.
    LaunchedEffect(event?.organizerId) {
        val orgId = event?.organizerId ?: return@LaunchedEffect
        runCatching { apiService.followStatus(orgId) }.onSuccess { following = it.following }
    }

    // Favoriten-Status unabhängig von den Eventdaten laden, da er sich ändern kann ohne dass das Event neu lädt.
    LaunchedEffect(eventId) {
        runCatching { apiService.favoriteStatus(eventId) }.onSuccess { favorited = it.favorited }
    }

    if (loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val currentEvent = event
    if (currentEvent == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Event nicht gefunden", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val isFree = currentEvent.price == "Kostenlos"
    val priceValue = currentEvent.priceValue

    /**
     * Erstellt für jedes gewählte Ticket einen eigenen Request (Backend kennt keinen
     * "quantity"-Parameter). Schlägt ein Request mittendrin fehl (z. B. Event wird
     * ausgebucht), zählt [created] die bereits erfolgreich erstellten Tickets: Wurde
     * mindestens eines erstellt, wird zu den Tickets navigiert (der echte Stand wird
     * dort angezeigt); andernfalls wird der Fehler gemeldet.
     */
    fun handlePurchase() {
        purchaseError = ""
        purchasing = true
        scope.launch {
            var created = 0
            var failure: Exception? = null
            try {
                repeat(quantity) {
                    apiService.createTicket(CreateTicketRequest(currentEvent.id))
                    created++
                }
            } catch (e: Exception) {
                failure = e
            }
            purchasing = false
            if (created > 0) {
                showTicketSheet = false
                navController.navigate(Routes.TICKETS) {
                    popUpTo(Routes.MAP)
                }
            } else {
                purchaseError = failure?.toApiErrorMessage("Kauf fehlgeschlagen")
                    ?: "Kauf fehlgeschlagen"
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f).verticalScrollWorkaround()) {
            Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                val img = imageUrl(currentEvent.imagePath)
                if (img != null) {
                    AsyncImage(
                        model = img,
                        contentDescription = currentEvent.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent, Color.Black.copy(alpha = 0.25f)),
                                ),
                            ),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(MeetNGoColors.BrandTeal, MeetNGoColors.BrandCoral))),
                    )
                }

                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .padding(16.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape),
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Zurück")
                }

                // Favoriten-Button: ruft je nach aktuellem Zustand favorite/unfavorite auf (optimistisches UI-Update entfällt bewusst,
                // der Zustand wird erst nach erfolgreicher Server-Antwort aktualisiert).
                IconButton(
                    onClick = {
                        scope.launch {
                            val result = runCatching {
                                if (favorited) apiService.unfavoriteEvent(eventId)
                                else apiService.favoriteEvent(eventId)
                            }.getOrNull()
                            if (result != null) favorited = result.favorited
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape),
                ) {
                    Icon(
                        if (favorited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (favorited) "Aus Favoriten entfernen" else "Zu Favoriten hinzufügen",
                        tint = if (favorited) MeetNGoColors.BrandCoral else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .offset(y = (-24).dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column {
                        Text(currentEvent.name, style = MaterialTheme.typography.headlineSmall)
                        Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                                Icon(
                                    Icons.Filled.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    formatDateLong(currentEvent.date),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                                Icon(
                                    Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    currentEvent.location,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MeetNGoColors.BrandTeal.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    (currentEvent.organizer ?: "?").take(2).uppercase(),
                                    color = MeetNGoColors.BrandTeal,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                            Column {
                                Text(currentEvent.organizer ?: "Unbekannt", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "Veranstalter",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        // Folgen-Button nur sichtbar, wenn ein Veranstalter zugeordnet ist.
                        val organizerId = currentEvent.organizerId
                        if (organizerId != null) {
                            OutlinedButton(onClick = {
                                scope.launch {
                                    val result = runCatching {
                                        if (following) apiService.unfollowUser(organizerId)
                                        else apiService.followUser(organizerId)
                                    }.getOrNull()
                                    if (result != null) following = result.following
                                }
                            }) {
                                Icon(
                                    if (following) Icons.Filled.Check else Icons.Filled.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(if (following) " Gefolgt" else " Folgen")
                            }
                        }
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                Icons.Filled.People,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text("${currentEvent.attendees} Teilnehmer", style = MaterialTheme.typography.bodySmall)
                        }
                        // Dekorative, sich überlappende Platzhalter-Avatare (keine echten Teilnehmerbilder/-daten).
                        Row(modifier = Modifier.padding(top = 8.dp)) {
                            repeat(5) { i ->
                                Box(
                                    modifier = Modifier
                                        .offset(x = (-8 * i).dp)
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MeetNGoColors.BrandTeal.copy(alpha = 0.2f)),
                                )
                            }
                        }
                    }

                    Column {
                        Text("Beschreibung", style = MaterialTheme.typography.titleMedium)
                        Text(
                            currentEvent.description ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    if (!isFree) {
                        Column {
                            HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Preis pro Ticket", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${currentEvent.price} €", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider()
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Button(
                onClick = { showTicketSheet = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MeetNGoColors.BrandCoral),
            ) {
                Text(if (isFree) "Kostenlos teilnehmen" else "Ticket kaufen")
            }
        }
    }

    if (showTicketSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { showTicketSheet = false }, sheetState = sheetState) {
            Column(modifier = Modifier.padding(16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(
                    if (isFree) "Teilnahme bestätigen" else "Ticket kaufen",
                    style = MaterialTheme.typography.titleMedium,
                )

                Column {
                    Text("Ticketart", style = MaterialTheme.typography.labelLarge)
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        ),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text("Standard Ticket")
                                Text(
                                    "Zugang zum Event",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                if (isFree) "Kostenlos" else "${currentEvent.price} €",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                Column {
                    Text("Anzahl", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        OutlinedButton(
                            onClick = { if (quantity > 1) quantity-- },
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = CircleShape,
                        ) { Text("-", style = MaterialTheme.typography.titleMedium) }
                        Text("$quantity", style = MaterialTheme.typography.titleLarge, modifier = Modifier.size(48.dp), textAlign = TextAlign.Center)
                        OutlinedButton(
                            onClick = { quantity++ },
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = CircleShape,
                        ) { Text("+", style = MaterialTheme.typography.titleMedium) }
                    }
                }

                // Zahlungsmethoden sind aktuell rein dekorativ (UI-Mockup), es gibt keine echte Auswahl-Logik.
                if (!isFree) {
                    Column {
                        Text("Zahlungsmethode", style = MaterialTheme.typography.labelLarge)
                        Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Card(
                                border = BorderStroke(1.dp, MeetNGoColors.BrandTeal),
                                colors = CardDefaults.cardColors(
                                    containerColor = MeetNGoColors.BrandTeal.copy(alpha = 0.05f),
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("PayPal", modifier = Modifier.padding(16.dp)) }
                            Card(
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Kreditkarte", modifier = Modifier.padding(16.dp)) }
                            Card(
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Google Pay", modifier = Modifier.padding(16.dp)) }
                        }
                    }
                }

                Column {
                    HorizontalDivider()
                    if (!isFree) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Gesamt")
                            Text(
                                "%.2f €".format(priceValue * quantity).replace(".", ","),
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }
                    }
                    if (purchaseError.isNotBlank()) {
                        Text(
                            purchaseError,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                    Button(
                        onClick = { handlePurchase() },
                        enabled = !purchasing,
                        modifier = Modifier.fillMaxWidth().height(48.dp).padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MeetNGoColors.BrandCoral),
                    ) {
                        if (purchasing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        } else {
                            Text(if (isFree) "Bestätigen" else "Jetzt kaufen")
                        }
                    }
                }
            }
        }
    }
}

/** Kleiner Helfer, um vertikales Scrollen als Modifier-Extension anzuwenden (vermeidet wiederholten Boilerplate-Code). */
@Composable
private fun Modifier.verticalScrollWorkaround(): Modifier {
    val scrollState = rememberScrollState()
    return this.then(verticalScroll(scrollState))
}
