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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.meetngo.app.data.api.ApiService
import com.meetngo.app.data.api.BASE_URL
import com.meetngo.app.data.model.Event
import com.meetngo.app.ui.navigation.Routes
import com.meetngo.app.ui.theme.MeetNGoColors
import com.meetngo.app.util.formatDateMedium

private val CATEGORIES = listOf("Alle", "Musik", "Sport", "Kunst", "Food", "Tech", "Outdoor")

/** Verknüpft den für die API benötigten Sortier-Wert mit seinem deutschen Anzeigelabel. */
private data class SortOption(val value: String, val label: String)

private val SORT_OPTIONS = listOf(
    SortOption("date", "Datum"),
    SortOption("name", "Name"),
    SortOption("attendees", "Beliebtheit"),
    SortOption("price", "Preis"),
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
 * sowie ein Bottom-Sheet mit Sortierung, Reihenfolge und Preisfilter.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavHostController, apiService: ApiService) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Alle") }
    var sort by remember { mutableStateOf("date") }
    var order by remember { mutableStateOf("asc") }
    var priceFilter by remember { mutableStateOf("") } // "", "free", "paid"
    var showFilterSheet by remember { mutableStateOf(false) }

    var events by remember { mutableStateOf<List<Event>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Zählt nur "von der Standardeinstellung abweichende" Filter, damit das Badge nicht ständig "3" zeigt.
    val activeFilterCount = listOf(priceFilter.isNotEmpty(), sort != "date", order != "asc").count { it }

    // Lädt die Events bei jeder Änderung eines Filters neu; das Debounce (delay) verhindert
    // einen Request pro Tastenanschlag bei der Freitextsuche.
    LaunchedEffect(searchQuery, selectedCategory, sort, order, priceFilter) {
        loading = true
        kotlinx.coroutines.delay(300)
        error = null
        try {
            events = apiService.getEvents(
                search = searchQuery.ifBlank { null },
                category = if (selectedCategory == "Alle") null else selectedCategory,
                sort = sort,
                order = order,
                priceFilter = priceFilter.ifBlank { null },
            )
        } catch (e: Exception) {
            events = emptyList()
            error = "Fehler beim Laden der Events"
        } finally {
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Suche",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                BadgedBox(
                    badge = {
                        if (activeFilterCount > 0) {
                            Badge(containerColor = MeetNGoColors.BrandCoral) { Text("$activeFilterCount") }
                        }
                    },
                ) {
                    OutlinedButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" Filter")
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Events, Orte, Kategorien...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )

            Row(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CATEGORIES.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MeetNGoColors.BrandTeal,
                            selectedLabelColor = Color.White,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedCategory == category,
                            borderColor = MaterialTheme.colorScheme.outline,
                            selectedBorderColor = MeetNGoColors.BrandTeal,
                        ),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .clickable { showFilterSheet = true },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    "Sortiert nach:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${SORT_OPTIONS.first { it.value == sort }.label} (${if (order == "asc") "↑" else "↓"})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MeetNGoColors.BrandTeal,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        if (loading) {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Text(
                error ?: "",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            Text(
                "${events.size} Event${if (events.size != 1) "s" else ""} gefunden",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            if (events.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
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
                    modifier = Modifier.fillMaxSize(),
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

    if (showFilterSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }, sheetState = sheetState) {
            Column(modifier = Modifier.padding(16.dp).padding(bottom = 24.dp)) {
                Text("Sortierung & Filter", style = MaterialTheme.typography.titleMedium)

                Text(
                    "Sortieren nach",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
                )
                FilterGrid(
                    options = SORT_OPTIONS.map { it.label to (it.value == sort) },
                    onSelect = { index -> sort = SORT_OPTIONS[index].value },
                )

                Text(
                    "Reihenfolge",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
                )
                FilterGrid(
                    options = listOf("Aufsteigend ↑" to (order == "asc"), "Absteigend ↓" to (order == "desc")),
                    onSelect = { index -> order = if (index == 0) "asc" else "desc" },
                )

                Text(
                    "Preis",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
                )
                val priceOptions = listOf("" to "Alle", "free" to "Kostenlos", "paid" to "Kostenpflichtig")
                FilterGrid(
                    options = priceOptions.map { it.second to (it.first == priceFilter) },
                    onSelect = { index -> priceFilter = priceOptions[index].first },
                )

                Row(modifier = Modifier.padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = {
                            sort = "date"
                            order = "asc"
                            priceFilter = ""
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("Zurücksetzen") }
                    Button(
                        onClick = { showFilterSheet = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MeetNGoColors.BrandCoral),
                        modifier = Modifier.weight(1f),
                    ) { Text("Anwenden") }
                }
            }
        }
    }
}

/** Stellt eine Liste von Optionen als zweispaltiges Raster von Auswahl-Buttons dar (für das Filter-Bottom-Sheet). */
@Composable
private fun FilterGrid(options: List<Pair<String, Boolean>>, onSelect: (Int) -> Unit) {
    val rows = options.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEachIndexed { i, (label, selected) ->
                    val index = options.indexOfFirst { it.first == label }
                    OutlinedButton(
                        onClick = { onSelect(index) },
                        colors = if (selected) {
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = MeetNGoColors.BrandTeal.copy(alpha = 0.1f),
                                contentColor = MeetNGoColors.BrandTeal,
                            )
                        } else {
                            ButtonDefaults.outlinedButtonColors()
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text(label) }
                }
            }
        }
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
