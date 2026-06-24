@file:OptIn(ExperimentalMaterial3Api::class)

package com.meetngo.app.ui.screens.organizer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.meetngo.app.data.api.ApiService
import com.meetngo.app.data.api.BASE_URL
import com.meetngo.app.data.model.Event
import com.meetngo.app.data.model.UpdateEventRequest
import com.meetngo.app.data.repository.AuthRepository
import com.meetngo.app.ui.components.SkeletonLine
import com.meetngo.app.ui.components.shimmer
import com.meetngo.app.ui.navigation.Routes
import com.meetngo.app.ui.screens.auth.toAuthErrorMessage
import com.meetngo.app.ui.theme.MeetNGoColors
import com.meetngo.app.util.formatDateShort
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Baut aus einem relativen Backend-Pfad eine vollständige Bild-URL; absolute URLs werden unverändert übernommen. */
private fun imageUrl(imagePath: String?): String? {
    if (imagePath.isNullOrBlank()) return null
    if (imagePath.startsWith("http")) return imagePath
    return BASE_URL.removeSuffix("/") + "/" + imagePath.trimStart('/')
}

/** Geschätzter Umsatz eines Events = Ticketpreis × Anzahl Teilnehmer (0 bei kostenlosen Events). */
private fun eventRevenue(event: Event): Double = event.priceValue * event.attendees

/** Formatiert einen Geldbetrag im deutschen Format ohne Nachkommastellen, z. B. "6.786 €". */
private fun formatEuro(value: Double): String = String.format(Locale.GERMANY, "%,.0f €", value)

/** Kurzes Countdown-Label bis zum Event, z. B. "Heute", "Morgen", "in 5 T." oder "Vorbei". */
private fun daysUntilLabel(dateStr: String): String {
    val eventDate = runCatching { LocalDateTime.parse(dateStr).toLocalDate() }.getOrNull() ?: return "—"
    val days = ChronoUnit.DAYS.between(LocalDate.now(), eventDate)
    return when {
        days < 0 -> "Vorbei"
        days == 0L -> "Heute"
        days == 1L -> "Morgen"
        else -> "in $days T."
    }
}

/**
 * Dashboard für Veranstalter: zeigt alle eigenen Veranstaltungen mit einem
 * Auswahl-Tab-Leiste (bei mehreren Events), Statistiken und Aktionen
 * (hervorheben, aktivieren/deaktivieren, löschen) für das jeweils gewählte Event.
 */
@Composable
fun OrganizerDashboardScreen(
    navController: NavHostController,
    apiService: ApiService,
    authRepository: AuthRepository,
) {
    val user by authRepository.userFlow.collectAsState(initial = null)
    var myEvents by remember { mutableStateOf<List<Event>>(emptyList()) }
    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Lädt alle Events, deren organizerId dem eingeloggten Benutzer entspricht, und wählt das erste vor.
    LaunchedEffect(user?.id) {
        val uid = user?.id
        if (uid != null) {
            runCatching { apiService.getEvents(organizerId = uid) }
                .onSuccess { events ->
                    myEvents = events
                    if (events.isNotEmpty()) selectedEvent = events.first()
                }
                .onFailure { error = it.toAuthErrorMessage("Events konnten nicht geladen werden") }
        }
        loading = false
    }

    /** Schaltet das Featured-Flag des aktuell gewählten Events um und aktualisiert lokale Liste + Auswahl. */
    fun handleToggleFeatured() {
        val ev = selectedEvent ?: return
        scope.launch {
            runCatching { apiService.updateEvent(ev.id, UpdateEventRequest(featured = ev.featured != 1)) }
                .onSuccess { updated ->
                    myEvents = myEvents.map { if (it.id == updated.id) updated else it }
                    selectedEvent = updated
                }
                .onFailure { error = it.toAuthErrorMessage("Aktion fehlgeschlagen") }
        }
    }

    /** Schaltet das Aktiv-Flag des aktuell gewählten Events um und aktualisiert lokale Liste + Auswahl. */
    fun handleToggleActive() {
        val ev = selectedEvent ?: return
        scope.launch {
            runCatching { apiService.updateEvent(ev.id, UpdateEventRequest(active = ev.active != 1)) }
                .onSuccess { updated ->
                    myEvents = myEvents.map { if (it.id == updated.id) updated else it }
                    selectedEvent = updated
                }
                .onFailure { error = it.toAuthErrorMessage("Aktion fehlgeschlagen") }
        }
    }

    /** Löscht das aktuell gewählte Event und navigiert anschließend immer zur Kartenansicht (auch bei Fehler). */
    fun handleDelete() {
        showDeleteDialog = false
        val event = selectedEvent ?: return
        scope.launch {
            try {
                apiService.deleteEvent(event.id)
            } catch (_: Exception) {
            }
            navController.navigate(Routes.MAP) { popUpTo(Routes.MAP) { inclusive = true } }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(text = "Event Dashboard") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Zurück")
                }
            },
        )

        if (loading) {
            DashboardSkeleton()
        } else if (myEvents.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Du hast noch keine Events erstellt.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = { navController.navigate(Routes.CREATE_EVENT) },
                    colors = ButtonDefaults.buttonColors(containerColor = MeetNGoColors.BrandCoral),
                ) {
                    Text(text = "Event erstellen")
                }
            }
        } else {
            val event = selectedEvent
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // Gesamtübersicht über alle Events des Veranstalters: Anzahl Events, Teilnehmer und
                // geschätzter Gesamtumsatz – gibt dem Veranstalter auf einen Blick die wichtigsten Zahlen.
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Gesamtübersicht", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        StatCard(
                            icon = Icons.Filled.Event,
                            label = "Events",
                            value = myEvents.size.toString(),
                            modifier = Modifier.weight(1f),
                        )
                        StatCard(
                            icon = Icons.Filled.People,
                            label = "Teilnehmer",
                            value = myEvents.sumOf { it.attendees }.toString(),
                            modifier = Modifier.weight(1f),
                        )
                        StatCard(
                            icon = Icons.Filled.Payments,
                            label = "Umsatz",
                            value = formatEuro(myEvents.sumOf { eventRevenue(it) }),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // Event-Auswahl-Tabs nur anzeigen, wenn der Veranstalter mehr als ein Event hat.
                if (myEvents.size > 1) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        myEvents.forEach { e ->
                            val selected = selectedEvent?.id == e.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (selected) MeetNGoColors.BrandTeal else MaterialTheme.colorScheme.surface)
                                    .border(if (selected) 0.dp else 1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
                                    .clickable { selectedEvent = e }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    text = e.name,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }

                if (event != null) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(16.dp)) {
                            val img = imageUrl(event.imagePath)
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                            ) {
                                if (img != null) {
                                    AsyncImage(
                                        model = img,
                                        contentDescription = event.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Brush.linearGradient(listOf(MeetNGoColors.BrandTeal, MeetNGoColors.BrandCoral))),
                                    )
                                }
                            }
                            Column(modifier = Modifier.padding(start = 16.dp)) {
                                Text(text = event.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                Text(
                                    text = formatDateShort(event.date),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Row(
                                    modifier = Modifier.padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Badge(
                                        containerColor = if (event.active == 1) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (event.active == 1) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    ) { Text(text = if (event.active == 1) "Aktiv" else "Inaktiv", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) }
                                    if (event.featured == 1) {
                                        Badge(
                                            containerColor = MeetNGoColors.BrandCoral,
                                            contentColor = Color.White,
                                        ) { Text(text = "★ Hervorgehoben", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) }
                                    }
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = "Statistiken", style = MaterialTheme.typography.titleMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            StatCard(
                                icon = Icons.Filled.People,
                                label = "Teilnehmer",
                                value = event.attendees.toString(),
                                modifier = Modifier.weight(1f),
                            )
                            StatCard(
                                icon = Icons.Filled.Payments,
                                label = "Umsatz",
                                value = if (event.priceValue > 0.0) formatEuro(eventRevenue(event)) else "Gratis",
                                modifier = Modifier.weight(1f),
                            )
                            StatCard(
                                icon = Icons.Filled.Schedule,
                                label = "Countdown",
                                value = daysUntilLabel(event.date),
                                modifier = Modifier.weight(1f),
                            )
                        }

                        // Auslastung: Fortschrittsbalken Teilnehmer/Kapazität, sonst Hinweis auf
                        // unbegrenzte Plätze. Gibt dem Veranstalter ein Gefühl, wie voll das Event ist.
                        val capacity = event.capacity
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Filled.Speed, contentDescription = null, tint = MeetNGoColors.BrandTeal, modifier = Modifier.size(18.dp))
                                        Text(text = "Auslastung", style = MaterialTheme.typography.titleSmall)
                                    }
                                    if (capacity != null && capacity > 0) {
                                        val percent = (event.attendees.toFloat() / capacity * 100).toInt().coerceIn(0, 100)
                                        Text(
                                            text = "$percent %",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MeetNGoColors.BrandTeal,
                                        )
                                    }
                                }
                                if (capacity != null && capacity > 0) {
                                    val fraction = (event.attendees.toFloat() / capacity).coerceIn(0f, 1f)
                                    LinearProgressIndicator(
                                        progress = { fraction },
                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                                        color = MeetNGoColors.BrandTeal,
                                        trackColor = MeetNGoColors.BrandTeal.copy(alpha = 0.15f),
                                    )
                                    val free = (capacity - event.attendees).coerceAtLeast(0)
                                    Text(
                                        text = "$free von $capacity Plätzen frei",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                } else {
                                    Text(
                                        text = "Unbegrenzte Plätze – ${event.attendees} Teilnehmer bisher",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    if (error.isNotBlank()) {
                        Text(text = error, color = MaterialTheme.colorScheme.error)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = "Aktionen", style = MaterialTheme.typography.titleMedium)
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ActionButton(
                                icon = Icons.Filled.Edit,
                                label = "Neues Event erstellen",
                                onClick = { navController.navigate(Routes.CREATE_EVENT) },
                            )
                            ActionButton(
                                icon = Icons.Filled.Star,
                                label = if (event.featured == 1) "Hervorhebung entfernen" else "Event hervorheben",
                                onClick = { handleToggleFeatured() },
                            )
                            ActionButton(
                                icon = if (event.active == 1) Icons.Filled.Cancel else Icons.Filled.CheckCircle,
                                label = if (event.active == 1) "Deaktivieren" else "Aktivieren",
                                onClick = { handleToggleActive() },
                            )
                            ActionButton(
                                icon = Icons.Filled.Delete,
                                label = "Löschen",
                                onClick = { showDeleteDialog = true },
                                destructive = true,
                            )
                        }
                    }

                    // Öffnet den Kamera-Scanner zum Einlösen von Ticket-QR-Codes.
                    Button(
                        onClick = { navController.navigate(Routes.SCANNER) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MeetNGoColors.BrandTeal),
                    ) {
                        Icon(Icons.Filled.QrCode, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text(text = " QR-Code Scanner öffnen")
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    }
                    Text(text = "Event löschen?", modifier = Modifier.padding(top = 12.dp))
                }
            },
            text = {
                Text(
                    text = "Diese Aktion kann nicht rückgängig gemacht werden. Alle Tickets werden storniert.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = { handleDelete() }) {
                    Text(text = "Löschen", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(text = "Abbrechen") }
            },
        )
    }
}

/** Einzeilige Aktionsschaltfläche mit Icon und Label; [destructive] färbt sie rot (z. B. für "Löschen"). */
@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = label,
                color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Lade-Platzhalter für das Dashboard: imitiert Gesamtübersicht, Event-Karte und Statistik-Kacheln. */
@Composable
private fun DashboardSkeleton() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) {
                Card(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(modifier = Modifier.size(20.dp).shimmer(CircleShape))
                        SkeletonLine(widthFraction = 0.7f, height = 16.dp)
                        SkeletonLine(widthFraction = 0.9f, height = 10.dp)
                    }
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.size(80.dp).shimmer(RoundedCornerShape(12.dp)))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SkeletonLine(widthFraction = 0.6f, height = 18.dp)
                    SkeletonLine(widthFraction = 0.4f, height = 12.dp)
                    SkeletonLine(widthFraction = 0.3f, height = 12.dp)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) {
                Card(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        SkeletonLine(widthFraction = 0.6f, height = 18.dp)
                        SkeletonLine(widthFraction = 0.8f, height = 10.dp)
                    }
                }
            }
        }
    }
}

/** Kleine Statistik-Kachel mit Icon, Wert und Beschriftung, z. B. für Teilnehmerzahl/Kapazität/Preis. */
@Composable
private fun StatCard(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MeetNGoColors.BrandTeal,
                modifier = Modifier.size(20.dp),
            )
            Text(text = value, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
