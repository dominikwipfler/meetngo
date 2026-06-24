@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
)

package com.meetngo.app.ui.screens.createevent

import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.meetngo.app.data.api.ApiService
import com.meetngo.app.data.model.GeocodeResult
import com.meetngo.app.ui.navigation.Routes
import com.meetngo.app.ui.screens.auth.toAuthErrorMessage
import com.meetngo.app.ui.theme.MeetNGoColors
import com.meetngo.app.util.formatDateLong
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

private val CATEGORIES = listOf(
    "Musik", "Sport", "Kunst", "Food", "Tech", "Outdoor",
    "Familie", "Bildung", "Markt", "Stadtleben", "Nightlife", "Sonstiges",
)
private const val MAX_IMAGE_BYTES = 5 * 1024 * 1024

/**
 * Formular zum Anlegen einer neuen Veranstaltung durch einen Veranstalter.
 * Sendet die Felder zusammen mit einem optionalen Bild als Multipart-Request
 * (siehe [ApiService.createEvent]) und navigiert bei Erfolg zum Veranstalter-Dashboard.
 */
@Composable
fun CreateEventScreen(
    navController: NavHostController,
    apiService: ApiService,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var plzOrt by remember { mutableStateOf("") }
    var selectedLat by remember { mutableStateOf<Double?>(null) }
    var selectedLng by remember { mutableStateOf<Double?>(null) }
    var addressConfirmed by remember { mutableStateOf(false) }
    var addressSuggestions by remember { mutableStateOf<List<GeocodeResult>>(emptyList()) }
    var addressSearching by remember { mutableStateOf(false) }
    // Ob das Event kostenpflichtig ist; steuert, ob das Preisfeld angezeigt wird.
    var isPaid by remember { mutableStateOf(false) }
    var price by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(CATEGORIES.last()) }
    var featured by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pickedDateMillis by remember { mutableStateOf<Long?>(null) }
    // Pflichtfeld-Markierungen erscheinen erst nach dem ersten fehlgeschlagenen Veröffentlichen-Versuch,
    // nicht schon während der Nutzer das Formular noch ausfüllt.
    var attemptedSubmit by remember { mutableStateOf(false) }
    val nameFocusRequester = remember { FocusRequester() }
    val descriptionFocusRequester = remember { FocusRequester() }
    val streetFocusRequester = remember { FocusRequester() }
    val plzOrtFocusRequester = remember { FocusRequester() }
    val dateFocusRequester = remember { FocusRequester() }
    val priceFocusRequester = remember { FocusRequester() }

    // Öffnet den System-Bildauswahldialog und validiert Dateityp/-größe, bevor das Bild übernommen wird.
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        error = ""
        val resolver = context.contentResolver
        val type = resolver.getType(uri) ?: ""
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(type)?.lowercase()
        val allowedTypes = setOf("image/jpeg", "image/png", "image/webp")
        val allowedExts = setOf("jpg", "jpeg", "png", "webp")
        // Doppelte Prüfung über MIME-Type und Dateiendung, da manche Content-Provider keinen verlässlichen MIME-Type liefern.
        if (type !in allowedTypes && ext !in allowedExts) {
            error = "Nur JPG, PNG oder WebP Bilder erlaubt."
            return@rememberLauncherForActivityResult
        }
        val size = resolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
        if (size > MAX_IMAGE_BYTES) {
            error = "Datei ist zu groß. Maximum 5MB erlaubt."
            return@rememberLauncherForActivityResult
        }
        imageUri = uri
    }

    // Sucht Adressvorschläge, sobald sich Straße oder PLZ/Ort ändern. Das Debounce (delay)
    // verhindert einen Request pro Tastenanschlag; erst ab einer sinnvollen Eingabelänge wird gesucht.
    LaunchedEffect(street, plzOrt) {
        if (street.isBlank() || plzOrt.length < 4) {
            addressSuggestions = emptyList()
            addressSearching = false
            return@LaunchedEffect
        }
        delay(300)
        addressSearching = true
        addressSuggestions = try {
            apiService.geocode("$street, $plzOrt")
        } catch (_: Exception) {
            emptyList()
        } finally {
            addressSearching = false
        }
    }

    /** Übernimmt einen Adressvorschlag: setzt die Koordinaten und bestätigt die Adresse. */
    fun selectAddress(result: GeocodeResult) {
        selectedLat = result.lat
        selectedLng = result.lng
        addressConfirmed = true
        addressSuggestions = emptyList()
    }

    /**
     * Validiert Pflichtfelder, baut den Multipart-Request und veröffentlicht die Veranstaltung.
     * Bei einem fehlenden/ungültigen Pflichtfeld wird statt einer Sammel-Fehlermeldung direkt zum
     * ersten betroffenen Feld gescrollt/fokussiert (siehe [attemptedSubmit] und die `isError`-Flags
     * an den einzelnen Feldern weiter unten).
     */
    fun handlePublish() {
        error = ""
        attemptedSubmit = true
        if (name.isBlank()) {
            nameFocusRequester.requestFocus()
            return
        }
        if (description.isBlank()) {
            descriptionFocusRequester.requestFocus()
            return
        }
        if (street.isBlank()) {
            streetFocusRequester.requestFocus()
            return
        }
        if (plzOrt.isBlank()) {
            plzOrtFocusRequester.requestFocus()
            return
        }
        val lat = selectedLat
        val lng = selectedLng
        if (!addressConfirmed || lat == null || lng == null) {
            plzOrtFocusRequester.requestFocus()
            return
        }
        if (date.isBlank()) {
            dateFocusRequester.requestFocus()
            return
        }
        // Bei einem kostenpflichtigen Event muss ein gültiger Preis > 0 eingetragen sein.
        val priceValid = !isPaid || (price.replace(",", ".").toDoubleOrNull()?.let { it > 0.0 } == true)
        if (!priceValid) {
            priceFocusRequester.requestFocus()
            return
        }
        loading = true
        scope.launch {
            try {
                // Multipart-Felder müssen als RequestBody vom Typ "text/plain" gesendet werden.
                fun text(value: String) = value.toRequestBody("text/plain".toMediaType())
                val fields = mutableMapOf(
                    "name" to text(name),
                    "description" to text(description),
                    "date" to text(date),
                    "location" to text("$street, $plzOrt"),
                    "lat" to text(lat.toString()),
                    "lng" to text(lng.toString()),
                    // Kostenlose Events senden den Sonderwert "Kostenlos"; sonst den eingegebenen Preis.
                    "price" to text(if (isPaid) price else "Kostenlos"),
                    "capacity" to text(capacity.ifBlank { "" }),
                    "category" to text(category),
                    "featured" to text(featured.toString()),
                )

                // Bild ist optional: nur wenn der Benutzer eines ausgewählt hat, wird es als zusätzlicher Part angehängt.
                val imagePart = imageUri?.let { uri ->
                    val resolver = context.contentResolver
                    val type = resolver.getType(uri) ?: "image/jpeg"
                    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: ByteArray(0)
                    val body = bytes.toRequestBody(type.toMediaType())
                    MultipartBody.Part.createFormData("image", "upload", body)
                }

                apiService.createEvent(fields, imagePart)
                // Nach dem Veröffentlichen direkt aufs Dashboard. Dabei wird der Backstack bis zur
                // Kartenansicht (Startseite) aufgeräumt – also auch das "Event erstellen"-Formular und
                // das Profil entfernt –, damit der Zurück-Pfeil im Dashboard direkt zur Karte führt und
                // nicht erneut auf dem Formular landet.
                navController.navigate(Routes.ORGANIZER_DASHBOARD) {
                    popUpTo(Routes.MAP) { inclusive = false }
                    launchSingleTop = true
                }
            } catch (e: Exception) {
                error = e.toAuthErrorMessage("Fehler beim Erstellen des Events")
            } finally {
                loading = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(text = "Event erstellen") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Zurück")
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Bildbereich: zeigt Vorschau mit Entfernen-Button, falls ein Bild gewählt wurde, sonst eine Upload-Fläche.
            Text(text = "Event-Bild", style = MaterialTheme.typography.titleMedium)
            if (imageUri != null) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = rememberAsyncImagePainter(imageUri),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(192.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    IconButton(
                        onClick = { imageUri = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(MaterialTheme.colorScheme.error, CircleShape)
                            .size(36.dp),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Bild entfernen", tint = MaterialTheme.colorScheme.onError)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Upload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Bild hochladen (max. 5MB)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            val nameError = attemptedSubmit && name.isBlank()
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(text = "Titel *") },
                isError = nameError,
                supportingText = { if (nameError) Text("Bitte einen Titel eingeben") },
                modifier = Modifier.fillMaxWidth().focusRequester(nameFocusRequester),
            )

            val descriptionError = attemptedSubmit && description.isBlank()
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(text = "Beschreibung *") },
                isError = descriptionError,
                supportingText = { if (descriptionError) Text("Bitte eine Beschreibung eingeben") },
                modifier = Modifier.fillMaxWidth().focusRequester(descriptionFocusRequester),
                minLines = 3,
            )

            // Kategorie-Auswahl als anklickbare "Chips"; nur eine Kategorie kann aktiv sein.
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Kategorie", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CATEGORIES.forEach { cat ->
                        val selected = category == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (selected) MeetNGoColors.BrandTeal else MaterialTheme.colorScheme.surface,
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (selected) MeetNGoColors.BrandTeal else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(50),
                                )
                                .clickable { category = cat }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = cat,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            // Adresseingabe: Straße/Hausnummer + PLZ/Ort lösen eine Geocoding-Suche aus; erst
            // die Auswahl eines Vorschlags liefert verlässliche Koordinaten für die Karte.
            // Beide Felder werden zusätzlich rot markiert, wenn die Adresse zwar ausgefüllt,
            // aber noch kein Vorschlag aus der Liste bestätigt wurde.
            val addressNeedsSelection = attemptedSubmit && street.isNotBlank() && plzOrt.isNotBlank() && !addressConfirmed
            val streetError = attemptedSubmit && (street.isBlank() || addressNeedsSelection)
            val plzOrtError = attemptedSubmit && (plzOrt.isBlank() || addressNeedsSelection)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = street,
                    onValueChange = {
                        street = it
                        addressConfirmed = false
                        selectedLat = null
                        selectedLng = null
                    },
                    label = { Text(text = "Straße & Hausnummer *") },
                    isError = streetError,
                    supportingText = {
                        if (streetError) {
                            Text(if (street.isBlank()) "Pflichtfeld" else "Bitte einen Vorschlag aus der Liste auswählen")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(streetFocusRequester),
                )
                OutlinedTextField(
                    value = plzOrt,
                    onValueChange = {
                        plzOrt = it
                        addressConfirmed = false
                        selectedLat = null
                        selectedLng = null
                    },
                    label = { Text(text = "PLZ & Ort *") },
                    isError = plzOrtError,
                    supportingText = {
                        if (plzOrtError) {
                            Text(if (plzOrt.isBlank()) "Pflichtfeld" else "Bitte einen Vorschlag aus der Liste auswählen")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(plzOrtFocusRequester),
                )

                if (addressSuggestions.isNotEmpty() && !addressConfirmed) {
                    Text(
                        text = "Bitte die passende Adresse auswählen:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                    ) {
                        addressSuggestions.forEach { suggestion ->
                            Text(
                                text = suggestion.label,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectAddress(suggestion) }
                                    .padding(12.dp),
                            )
                        }
                    }
                }

                // Laufende Suche, "keine Treffer" und Bestätigung sichtbar machen, damit der Nutzer
                // nie ohne Rückmeldung dasteht und die für die Karte verwendeten Koordinaten sieht.
                when {
                    addressSearching && !addressConfirmed -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text(
                                text = "Adresse wird gesucht…",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    addressConfirmed && selectedLat != null && selectedLng != null -> {
                        Text(
                            text = "✓ Adresse bestätigt – wird so auf der Karte angezeigt " +
                                "(${"%.4f".format(selectedLat)}, ${"%.4f".format(selectedLng)})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MeetNGoColors.BrandTeal,
                        )
                    }
                    street.isNotBlank() && plzOrt.length >= 4 && addressSuggestions.isEmpty() -> {
                        Text(
                            text = "Keine Adresse gefunden – bitte Straße und PLZ/Ort prüfen.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            // Datum/Uhrzeit-Feld ist nur lesbar; der eigentliche Picker-Flow läuft über die Dialoge unten.
            val dateError = attemptedSubmit && date.isBlank()
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = if (date.isBlank()) "" else formatDateLong(date),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(text = "Datum & Uhrzeit *") },
                    placeholder = { Text(text = "Datum & Uhrzeit wählen") },
                    trailingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                    isError = dateError,
                    supportingText = { if (dateError) Text("Bitte Datum & Uhrzeit auswählen") },
                    modifier = Modifier.fillMaxWidth().focusRequester(dateFocusRequester),
                )
                // Transparenter Klick-Layer: öffnet den Date-Picker statt der Tastatur.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showDatePicker = true },
                )
            }

            // Kosten: zuerst auswählen, ob das Event kostenlos oder kostenpflichtig ist; nur bei
            // "Kostenpflichtig" erscheint das Preisfeld (Pflichtangabe > 0).
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Kosten", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !isPaid,
                        onClick = { isPaid = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MeetNGoColors.BrandTeal.copy(alpha = 0.15f),
                            activeContentColor = MeetNGoColors.BrandTeal,
                        ),
                    ) { Text("Kostenlos") }
                    SegmentedButton(
                        selected = isPaid,
                        onClick = { isPaid = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MeetNGoColors.BrandTeal.copy(alpha = 0.15f),
                            activeContentColor = MeetNGoColors.BrandTeal,
                        ),
                    ) { Text("Kostenpflichtig") }
                }

                if (isPaid) {
                    val priceError = attemptedSubmit &&
                        (price.replace(",", ".").toDoubleOrNull()?.let { it > 0.0 } != true)
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text(text = "Ticketpreis (€) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = priceError,
                        supportingText = { if (priceError) Text("Bitte einen Preis größer als 0 eingeben") },
                        modifier = Modifier.fillMaxWidth().focusRequester(priceFocusRequester),
                    )
                }
            }

            OutlinedTextField(
                value = capacity,
                onValueChange = { capacity = it },
                label = { Text(text = "Kontingent (optional)") },
                placeholder = { Text(text = "z. B. 100 – leer lassen für unbegrenzt") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Event hervorheben")
                    Text(
                        text = "Mehr Sichtbarkeit für dein Event",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = featured, onCheckedChange = { featured = it })
            }

            if (error.isNotBlank()) {
                Text(text = error, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = { handlePublish() },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MeetNGoColors.BrandCoral,
                ),
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                }
                Text(text = if (loading) "Wird veröffentlicht..." else "Veröffentlichen")
            }
        }
    }

    // Zweistufiger Auswahlfluss: zuerst Datum, danach automatisch die Uhrzeit-Auswahl.
    if (showDatePicker) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = pickedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickedDateMillis = dateState.selectedDateMillis
                    showDatePicker = false
                    // Nach der Datumsauswahl direkt mit der Uhrzeitauswahl fortfahren.
                    if (pickedDateMillis != null) showTimePicker = true
                }) { Text("Weiter") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Abbrechen") }
            },
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickedDateMillis?.let { millis ->
                        // DatePicker liefert Millis in UTC; lokale Stunde/Minute aus dem TimePicker werden draufgesetzt
                        // und als ISO-LocalDateTime-String gespeichert (passend zum Backend-Format).
                        val localDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        date = localDate.atTime(timeState.hour, timeState.minute)
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
                    }
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Abbrechen") }
            },
            title = { Text("Uhrzeit wählen") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timeState)
                }
            },
        )
    }
}
