@file:OptIn(ExperimentalMaterial3Api::class)

package com.meetngo.app.ui.screens.tickets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.meetngo.app.data.api.ApiService
import com.meetngo.app.data.api.BASE_URL
import com.meetngo.app.data.model.Ticket
import com.meetngo.app.ui.navigation.Routes
import com.meetngo.app.ui.theme.MeetNGoColors
import com.meetngo.app.util.formatDateShort
import kotlinx.coroutines.launch

/** Baut aus einem relativen Backend-Pfad eine vollständige Bild-URL; absolute URLs werden unverändert übernommen. */
private fun imageUrl(imagePath: String?): String? {
    if (imagePath.isNullOrBlank()) return null
    if (imagePath.startsWith("http")) return imagePath
    return BASE_URL.removeSuffix("/") + "/" + imagePath.trimStart('/')
}

/** Parst das Event-Datum zu Millisekunden für den Aktiv/Vergangen-Vergleich; bei Parse-Fehlern wird Long.MAX_VALUE verwendet, damit das Ticket als "aktiv" gilt statt fälschlich zu verschwinden. */
private fun parseEventDateMillis(dateStr: String): Long {
    return runCatching {
        java.time.LocalDateTime.parse(dateStr).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    }.getOrDefault(Long.MAX_VALUE)
}

/**
 * Ticket-Übersicht des Benutzers mit zwei Tabs ("Aktiv"/"Vergangen"), QR-Code-Anzeige
 * pro Ticket (für den Einlass) und der Möglichkeit, aktive Tickets zu stornieren.
 */
@Composable
fun TicketsScreen(navController: NavHostController, apiService: ApiService) {
    var tickets by remember { mutableStateOf<List<Ticket>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var qrTicket by remember { mutableStateOf<Ticket?>(null) }
    var cancelTarget by remember { mutableStateOf<Ticket?>(null) }
    val scope = rememberCoroutineScope()

    // Lädt alle Tickets des eingeloggten Benutzers einmalig beim Öffnen des Screens.
    LaunchedEffect(Unit) {
        try {
            tickets = apiService.getMyTickets()
        } catch (_: Exception) {
            tickets = emptyList()
        } finally {
            loading = false
        }
    }

    /** Storniert ein Ticket über die API und entfernt es bei Erfolg aus der lokalen Liste. */
    fun handleCancel(ticket: Ticket) {
        scope.launch {
            runCatching { apiService.deleteTicket(ticket.id) }
                .onSuccess { tickets = tickets.filterNot { it.id == ticket.id } }
            cancelTarget = null
        }
    }

    // Teilt die Tickets anhand des Event-Datums in "aktiv" (Event liegt noch vor uns) und "vergangen" auf.
    val now = System.currentTimeMillis()
    val activeTickets = tickets.filter { parseEventDateMillis(it.eventDate) >= now }
    val pastTickets = tickets.filter { parseEventDateMillis(it.eventDate) < now }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Meine Tickets",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp),
        )

        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Aktiv") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Vergangen") })
        }

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (selectedTab) {
                    0 -> {
                        if (activeTickets.isEmpty()) {
                            item { EmptyState("Keine aktiven Tickets") }
                        } else {
                            items(activeTickets, key = { it.id }) { ticket ->
                                ActiveTicketCard(
                                    ticket = ticket,
                                    onShowQr = { qrTicket = ticket },
                                    onCancel = { cancelTarget = ticket },
                                    onClick = { navController.navigate(Routes.eventDetail(ticket.eventId)) },
                                )
                            }
                        }
                    }
                    1 -> {
                        if (pastTickets.isEmpty()) {
                            item { EmptyState("Keine vergangenen Tickets") }
                        } else {
                            items(pastTickets, key = { it.id }) { ticket -> PastTicketCard(ticket = ticket) }
                        }
                    }
                }
            }
        }
    }

    qrTicket?.let { ticket ->
        AlertDialog(
            onDismissRequest = { qrTicket = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Dein Ticket")
                    IconButton(onClick = { qrTicket = null }) {
                        Icon(Icons.Filled.Close, contentDescription = "Schließen")
                    }
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // QR-Code enthält Ticket- und Event-ID als JSON, damit der Scanner am Einlass das Ticket eindeutig zuordnen kann.
                    val qrContent = """{"ticketId":${ticket.id},"eventId":${ticket.eventId}}"""
                    val bitmap = remember(ticket.id) { generateQrCodeBitmap(qrContent, 512) }
                    Box(
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                    ) {
                        Image(bitmap = bitmap.asImageBitmap(), contentDescription = "QR-Code", modifier = Modifier.size(220.dp))
                    }
                    Text(ticket.eventName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                    Text(
                        formatDateShort(ticket.eventDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Ticket #${ticket.id}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {},
        )
    }

    cancelTarget?.let { ticket ->
        AlertDialog(
            onDismissRequest = { cancelTarget = null },
            title = { Text("Ticket stornieren?") },
            text = { Text("Möchtest du dein Ticket für \"${ticket.eventName}\" wirklich stornieren?") },
            confirmButton = {
                TextButton(onClick = { handleCancel(ticket) }) {
                    Text("Stornieren", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { cancelTarget = null }) { Text("Abbrechen") }
            },
        )
    }
}

/** Platzhalter-Anzeige (Icon + Text), wenn der aktive bzw. vergangene Tab keine Tickets enthält. */
@Composable
private fun EmptyState(text: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Filled.ConfirmationNumber,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Karte für ein aktives Ticket mit Event-Vorschau, Status-Badge sowie Buttons zum Anzeigen des QR-Codes und zum Stornieren. */
@Composable
private fun ActiveTicketCard(
    ticket: Ticket,
    onShowQr: () -> Unit,
    onCancel: () -> Unit,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row {
                val img = imageUrl(ticket.eventImagePath)
                if (img != null) {
                    AsyncImage(
                        model = img,
                        contentDescription = ticket.eventName,
                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MeetNGoColors.BrandTeal),
                    )
                }
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(ticket.eventName, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                        val (statusLabel, statusColor) = when (ticket.status) {
                            "used" -> "Verwendet" to Color(0xFF6B7280)
                            else -> "Aktiv" to Color(0xFF22C55E)
                        }
                        Badge(containerColor = statusColor) {
                            Text(statusLabel)
                        }
                    }
                    Text(
                        formatDateShort(ticket.eventDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        ticket.eventLocation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 12.dp, bottom = 12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onShowQr, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(" QR-Code")
                }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(" Stornieren", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

/** Karte für ein vergangenes Ticket; optisch abgedunkelt (alpha) und ohne Aktions-Buttons, da das Event bereits stattgefunden hat. */
@Composable
private fun PastTicketCard(ticket: Ticket) {
    Card(modifier = Modifier.fillMaxWidth().alpha(0.6f)) {
        Row(modifier = Modifier.padding(16.dp)) {
            val img = imageUrl(ticket.eventImagePath)
            if (img != null) {
                AsyncImage(
                    model = img,
                    contentDescription = ticket.eventName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(MeetNGoColors.BrandTeal, MeetNGoColors.BrandCoral))),
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(ticket.eventName, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                    Badge(containerColor = Color(0xFF6B7280)) { Text("Vergangen") }
                }
                Text(
                    formatDateShort(ticket.eventDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    ticket.eventLocation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}
