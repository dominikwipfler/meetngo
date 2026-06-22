package com.meetngo.app.ui.screens.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.preference.PreferenceManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.meetngo.app.data.api.ApiService
import com.meetngo.app.data.api.BASE_URL
import com.meetngo.app.data.model.Event
import com.meetngo.app.ui.navigation.Routes
import com.meetngo.app.ui.theme.MeetNGoColors
import com.meetngo.app.util.formatDateShort
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/** Standardmittelpunkt der Karte (Karlsruhe), falls keine Events vorhanden sind. */
private val DEFAULT_CENTER = GeoPoint(49.0069, 8.4037)
private const val DEFAULT_ZOOM = 14.0

/**
 * Kachelquelle der Karte: zeigt nicht direkt auf den OSM-Tile-Server, sondern auf
 * den Kachel-Proxy des Backends (BASE_URL + "tiles/"). osmdroid setzt die URL als
 * baseUrl + z + "/" + x + "/" + y + ".png" zusammen, was exakt zur Backend-Route
 * /tiles/:z/:x/:y.png passt. So lädt die Karte auch im Emulator, der externe
 * Server nicht direkt erreicht (siehe backend/routes/tiles.js).
 */
private val MEETNGO_TILE_SOURCE = XYTileSource(
    "MeetNGoOSM",
    0,
    19,
    256,
    ".png",
    arrayOf(BASE_URL + "tiles/"),
    "© OpenStreetMap-Mitwirkende",
)

/** Baut aus einem relativen Backend-Pfad eine vollständige Bild-URL; absolute URLs werden unverändert übernommen. */
private fun imageUrl(imagePath: String?): String? {
    if (imagePath.isNullOrBlank()) return null
    if (imagePath.startsWith("http")) return imagePath
    return BASE_URL.removeSuffix("/") + "/" + imagePath.trimStart('/')
}

private val CATEGORIES = listOf("Musik", "Sport", "Kunst", "Food", "Tech", "Outdoor")

// Feste Farbe pro Kategorie für die Karten-Marker (siehe Figma-Design).
private val CATEGORY_COLORS = mapOf(
    "Musik" to Color(0xFF6366F1),
    "Sport" to Color(0xFF22C55E),
    "Food" to Color(0xFFF97316),
    "Tech" to Color(0xFF0EA5E9),
    "Kunst" to Color(0xFFEC4899),
    "Outdoor" to Color(0xFF84CC16),
    "Sonstiges" to MeetNGoColors.BrandTeal,
)

/** Liefert die Markerfarbe einer Kategorie, mit "Sonstiges" als Fallback für unbekannte Kategorien. */
private fun categoryColor(category: String): Color = CATEGORY_COLORS[category] ?: CATEGORY_COLORS["Sonstiges"]!!

/** Draws a simple colored circular pin bitmap per category — osmdroid has no CSS divIcon equivalent. */
private fun createMarkerBitmap(color: Color): Bitmap {
    val size = 72
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color.toArgb() }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = AndroidColor.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    val radius = size / 2f - 4f
    canvas.drawCircle(size / 2f, size / 2f, radius, fillPaint)
    canvas.drawCircle(size / 2f, size / 2f, radius, borderPaint)
    return bitmap
}

/**
 * Kartenansicht: zeigt alle Veranstaltungen als farbcodierte Marker auf einer
 * OSMDroid-Karte (OpenStreetMap), mit Suchfeld, Kategorie-Filtern und einem
 * horizontalen Karussell der gefilterten Events darunter.
 */
@Composable
fun MapScreen(navController: NavHostController, apiService: ApiService) {
    val context = LocalContext.current
    var events by remember { mutableStateOf<List<Event>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var activeFilters by remember { mutableStateOf(setOf<String>()) }
    var selectedEvent by remember { mutableStateOf<Event?>(null) }

    LaunchedEffect(Unit) {
        try {
            events = apiService.getEvents(sort = "date", order = "asc")
        } catch (_: Exception) {
            events = emptyList()
        } finally {
            isLoading = false
        }
    }

    // Kombiniert Freitextsuche (Name/Ort) und Kategorie-Filter; beide Bedingungen müssen erfüllt sein.
    val filtered = events.filter { event ->
        val q = searchQuery.trim().lowercase()
        val matchesQuery = q.isEmpty() || event.name.lowercase().contains(q) || event.location.lowercase().contains(q)
        val matchesFilter = activeFilters.isEmpty() || activeFilters.contains(event.category)
        matchesQuery && matchesFilter
    }

    // rememberUpdatedState hält den AndroidView-update-Block immer auf dem aktuellen Stand,
    // ohne dass die MapView (factory) bei jeder Änderung neu erzeugt werden muss.
    val currentFiltered = rememberUpdatedState(filtered)
    val currentSelected = rememberUpdatedState(selectedEvent)
    val currentOnMarkerTap = rememberUpdatedState<(Event) -> Unit>({ selectedEvent = it })

    // MapView außerhalb der factory halten, damit sie beim Verlassen des Screens
    // sauber freigegeben werden kann. osmdroid startet Tile-Downloader-Threads und
    // Caches, die ohne onDetach() als Memory-/Thread-Leak weiterlaufen würden.
    val mapView = remember {
        // Pflicht-Setup von osmdroid vor der View-Erzeugung: ohne Konfiguration/User-Agent
        // blockt der Tile-Server (OpenStreetMap) die Anfragen.
        Configuration.getInstance().load(
            context,
            PreferenceManager.getDefaultSharedPreferences(context),
        )
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(MEETNGO_TILE_SOURCE)
            setMultiTouchControls(true)
            controller.setZoom(DEFAULT_ZOOM)
            controller.setCenter(DEFAULT_CENTER)
        }
    }
    // osmdroid erwartet onResume()/onPause(), damit Tile-Download-Threads beim Verlassen/
    // Backgrounden des Screens pausiert und beim Zurückkehren wieder gestartet werden.
    // onDetach() gibt die View beim endgültigen Verlassen frei (Memory-/Thread-Leak vermeiden).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            // Liefert die einmalig erstellte, gehaltene MapView (View-Erzeugung ist teuer).
            factory = { mapView },
            // update läuft bei jeder Recomposition: Marker werden komplett neu aufgebaut, da
            // osmdroid keine deklarative Diff-Logik für Overlays bietet.
            update = { mapView ->
                mapView.overlays.clear()

                currentFiltered.value.forEach { event ->
                    val marker = Marker(mapView).apply {
                        position = GeoPoint(event.lat, event.lng)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        // Eigens gezeichnetes Bitmap statt Standard-Pin, um die Event-Kategorie farblich zu kennzeichnen.
                        setIcon(
                            BitmapDrawable(
                                mapView.context.resources,
                                createMarkerBitmap(categoryColor(event.category)),
                            ),
                        )
                        title = event.name
                        snippet = event.location
                        setOnMarkerClickListener { _, _ ->
                            currentOnMarkerTap.value(event)
                            true
                        }
                    }
                    mapView.overlays.add(marker)
                }

                // Kamera folgt der Auswahl: ausgewähltes Event > erstes gefiltertes Event > Standardmittelpunkt.
                val target = currentSelected.value
                if (target != null) {
                    mapView.controller.animateTo(GeoPoint(target.lat, target.lng))
                } else if (currentFiltered.value.isNotEmpty()) {
                    val first = currentFiltered.value.first()
                    mapView.controller.setCenter(GeoPoint(first.lat, first.lng))
                } else {
                    mapView.controller.setCenter(DEFAULT_CENTER)
                }

                mapView.invalidate()
            },
        )

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        // Search bar + category filter chips, floating over the map (web: absolute top-0).
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Events suchen...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), RoundedCornerShape(50)),
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CATEGORIES.forEach { category ->
                    val selected = activeFilters.contains(category)
                    FilterChip(
                        selected = selected,
                        onClick = {
                            activeFilters = if (selected) activeFilters - category else activeFilters + category
                        },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
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
        }

        // Horizontal event card carousel, floating over the map (web: absolute bottom-0).
        LazyRow(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 12.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
            if (filtered.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Keine Events gefunden",
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(filtered, key = { it.id }) { event ->
                    val img = imageUrl(event.imagePath)
                    val isActive = selectedEvent?.id == event.id
                    Card(
                        modifier = Modifier
                            .size(width = 240.dp, height = 96.dp)
                            .padding(0.dp)
                            .then(
                                if (isActive) {
                                    Modifier.border(2.dp, MeetNGoColors.BrandTeal, RoundedCornerShape(12.dp))
                                } else {
                                    Modifier
                                },
                            ),
                        onClick = {
                            selectedEvent = event
                            navController.navigate(Routes.eventDetail(event.id))
                        },
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                            ) {
                                if (img != null) {
                                    AsyncImage(
                                        model = img,
                                        contentDescription = null,
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
                            Column {
                                Text(event.name, maxLines = 1, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    event.location,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    formatDateShort(event.date),
                                    maxLines = 1,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
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
        }

        FloatingActionButton(
            onClick = { navController.navigate(Routes.CREATE_EVENT) },
            containerColor = MeetNGoColors.BrandCoral,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 132.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Event erstellen")
        }
    }
}
