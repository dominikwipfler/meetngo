@file:OptIn(ExperimentalMaterial3Api::class)

package com.meetngo.app.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.meetngo.app.data.api.ApiService
import com.meetngo.app.data.api.BASE_URL
import com.meetngo.app.data.model.Event
import com.meetngo.app.ui.components.EventCardSkeleton
import com.meetngo.app.ui.components.EventFilterSheet
import com.meetngo.app.ui.components.EventFilters
import com.meetngo.app.ui.components.FilterButtonWithBadge
import com.meetngo.app.ui.components.SORT_OPTIONS
import com.meetngo.app.ui.components.resolveDateRange
import com.meetngo.app.ui.navigation.Routes
import com.meetngo.app.ui.theme.MeetNGoColors
import com.meetngo.app.util.formatDateMedium

private val CATEGORIES = listOf(
    "Alle", "Musik", "Sport", "Kunst", "Food", "Tech", "Outdoor",
    "Familie", "Bildung", "Markt", "Stadtleben", "Nightlife", "Sonstiges",
)

/** Baut aus einem relativen Backend-Pfad eine vollständige Bild-URL; absolute URLs werden unverändert übernommen. */
private fun imageUrl(imagePath: String?): String? {
    if (imagePath.isNullOrBlank()) return null
    if (imagePath.startsWith("http")) return imagePath
    val base = BASE_URL.removeSuffix("/")
    return base + "/" + imagePath.trimStart('/')
}

/**
 * Such- und Filterbildschirm für Veranstaltungen: Freitextsuche, Kategorie-Chips
 * sowie ein Bottom-Sheet mit Sortierung, Reihenfolge und Preisfilter (siehe
 * [com.meetngo.app.ui.components.EventFilterSheet], auch von [com.meetngo.app.ui.screens.map.MapScreen] genutzt).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavHostController, apiService: ApiService) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Alle") }
    var filters by remember { mutableStateOf(EventFilters()) }
    var showFilterSheet by remember { mutableStateOf(false) }

    var events by remember { mutableStateOf<List<Event>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // Wird hochgezählt, um einen erneuten Ladeversuch auszulösen (Retry-Button / Pull-to-Refresh).
    var reloadKey by remember { mutableStateOf(0) }
    val pullState = rememberPullToRefreshState()

    // Gemeinsame Ladefunktion für die Debounce-Suche, den Retry-Button und Pull-to-Refresh.
    suspend fun loadEvents() {
        error = null
        try {
            val (dateFrom, dateTo) = filters.resolveDateRange()
            events = apiService.getEvents(
                search = searchQuery.ifBlank { null },
                category = if (selectedCategory == "Alle") null else selectedCategory,
                sort = filters.sort,
                order = filters.order,
                priceFilter = filters.priceFilter.ifBlank { null },
                priceMax = filters.maxPrice.takeIf { it > 0 },
                dateFrom = dateFrom,
                dateTo = dateTo,
            )
        } catch (e: Exception) {
            events = emptyList()
            error = "Fehler beim Laden der Events"
        }
    }

    // Lädt die Events bei jeder Änderung eines Filters neu; das Debounce (delay) verhindert
    // einen Request pro Tastenanschlag bei der Freitextsuche.
    LaunchedEffect(searchQuery, selectedCategory, filters, reloadKey) {
        loading = true
        kotlinx.coroutines.delay(300)
        loadEvents()
        loading = false
    }

    // Pull-to-Refresh: lädt die aktuelle Trefferliste neu und beendet danach die Wisch-Animation.
    LaunchedEffect(pullState.isRefreshing) {
        if (pullState.isRefreshing) {
            loadEvents()
            pullState.endRefresh()
        }
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Suche",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                FilterButtonWithBadge(activeCount = filters.activeCount, onClick = { showFilterSheet = true })
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Events, Orte, Kategorien...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Suche zurücksetzen")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MeetNGoColors.BrandTeal,
                    focusedLeadingIconColor = MeetNGoColors.BrandTeal,
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )

            Row(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CATEGORIES.forEach { category ->
                    val selected = selectedCategory == category
                    FilterChip(
                        selected = selected,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MeetNGoColors.BrandTeal,
                            selectedLabelColor = Color.White,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = MaterialTheme.colorScheme.outline,
                            selectedBorderColor = MeetNGoColors.BrandTeal,
                        ),
                    )
                }
            }

            // Kompakte, antippbare Zusammenfassung der aktiven Sortierung; öffnet das Filter-Sheet.
            Row(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MeetNGoColors.BrandTeal.copy(alpha = 0.10f))
                    .clickable { showFilterSheet = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    "Sortiert nach",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${SORT_OPTIONS.first { it.value == filters.sort }.label} ${if (filters.order == "asc") "↑" else "↓"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MeetNGoColors.BrandTeal,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // Ergebnisbereich bekommt die verbleibende Höhe als begrenzte weight-Fläche. Wichtig: ein
        // LazyColumn darf nicht mit fillMaxSize direkt in einer nicht begrenzten Column liegen –
        // beim Ein-/Ausblenden der Tastatur (IME-Inset-Animation) führt das sonst zu einer
        // Remeasure-Lawine, die den Main-Thread blockiert (ANR beim Tippen ins Suchfeld).
        // nestedScroll verbindet die Liste mit dem Pull-to-Refresh.
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .nestedScroll(pullState.nestedScrollConnection),
        ) {
            // Beim ersten Laden Skeleton-Platzhalter zeigen; beim Pull-to-Refresh übernimmt der
            // PullToRefreshContainer die Lade-Anzeige, daher dann keine Skeletons.
            val showSkeletons = loading && !pullState.isRefreshing
            when {
                showSkeletons -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        repeat(5) { EventCardSkeleton() }
                    }
                }
                error != null -> {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(error ?: "", color = MaterialTheme.colorScheme.error)
                            Button(
                                onClick = { reloadKey++ },
                                colors = ButtonDefaults.buttonColors(containerColor = MeetNGoColors.BrandTeal),
                            ) { Text("Erneut versuchen") }
                        }
                    }
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            "${events.size} Event${if (events.size != 1) "s" else ""} gefunden",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )

                        if (events.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Keine Events gefunden", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "Versuche es mit anderen Suchbegriffen oder Filtern",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(events, key = { it.id }) { event ->
                                    EventCard(event = event, onClick = {
                                        navController.navigate(Routes.eventDetail(event.id))
                                    })
                                }
                            }
                        }
                    }
                }
            }

            PullToRefreshContainer(state = pullState, modifier = Modifier.align(Alignment.TopCenter))
        }
    }

    if (showFilterSheet) {
        EventFilterSheet(
            filters = filters,
            onFiltersChange = { filters = it },
            onDismiss = { showFilterSheet = false },
        )
    }
}

/** Kompakte Trefferkarte in der Ergebnisliste: Bild-Thumbnail, Name, Datum, Ort, Kategorie-Badge, Teilnehmerzahl und Preis. */
@Composable
private fun EventCard(event: Event, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
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
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(event.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(formatDateMedium(event.date), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                Text(event.location, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ) {
                        Text(event.category, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                    Text("${event.attendees} Teilnehmer", style = MaterialTheme.typography.labelSmall)
                    Text(
                        if (event.price == "Kostenlos") "Kostenlos" else "${event.price} €",
                        style = MaterialTheme.typography.labelSmall,
                        color = MeetNGoColors.BrandTeal,
                    )
                }
            }
        }
    }
}
