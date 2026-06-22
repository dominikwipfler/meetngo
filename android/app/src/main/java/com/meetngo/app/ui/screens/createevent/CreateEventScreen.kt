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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.meetngo.app.data.api.ApiService
import com.meetngo.app.ui.navigation.Routes
import com.meetngo.app.ui.screens.auth.toAuthErrorMessage
import com.meetngo.app.ui.theme.MeetNGoColors
import com.meetngo.app.util.formatDateLong
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

private val CATEGORIES = listOf("Musik", "Sport", "Kunst", "Food", "Tech", "Outdoor", "Sonstiges")
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
    var location by remember { mutableStateOf("") }
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

    /** Validiert Pflichtfelder, baut den Multipart-Request und veröffentlicht die Veranstaltung. */
    fun handlePublish() {
        error = ""
        if (name.isBlank() || description.isBlank() || location.isBlank() || date.isBlank()) {
            error = "Bitte fülle alle Pflichtfelder aus"
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
                    "location" to text(location),
                    // Leeres Preisfeld bedeutet "kostenlos".
                    "price" to text(price.ifBlank { "Kostenlos" }),
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
                navController.navigate(Routes.ORGANIZER_DASHBOARD) {
                    // Entfernt den Profil-Screen aus dem Backstack, damit "Zurück" nicht mehr dorthin führt.
                    popUpTo(Routes.PROFILE)
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

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(text = "Titel *") },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(text = "Beschreibung *") },
                modifier = Modifier.fillMaxWidth(),
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

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text(text = "Ort *") },
                modifier = Modifier.fillMaxWidth(),
            )

            // Datum/Uhrzeit-Feld ist nur lesbar; der eigentliche Picker-Flow läuft über die Dialoge unten.
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = if (date.isBlank()) "" else formatDateLong(date),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(text = "Datum & Uhrzeit *") },
                    placeholder = { Text(text = "Datum & Uhrzeit wählen") },
                    trailingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                )
                // Transparenter Klick-Layer: öffnet den Date-Picker statt der Tastatur.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showDatePicker = true },
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text(text = "Ticketpreis (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "Leer lassen für kostenloses Event",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                    )
                }
                OutlinedTextField(
                    value = capacity,
                    onValueChange = { capacity = it },
                    label = { Text(text = "Kontingent") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }

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
