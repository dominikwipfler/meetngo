@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
)

package com.meetngo.app.ui.screens.profile

import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.meetngo.app.data.api.ApiService
import com.meetngo.app.data.model.ChangePasswordRequest
import com.meetngo.app.data.model.UpdateProfileRequest
import com.meetngo.app.data.repository.AuthRepository
import com.meetngo.app.ui.navigation.Routes
import com.meetngo.app.ui.screens.auth.toAuthErrorMessage
import com.meetngo.app.ui.theme.MeetNGoColors
import com.meetngo.app.ui.theme.ThemeState
import kotlinx.coroutines.launch

/** Kategorien, aus denen der Benutzer seine persönlichen Interessen auswählen kann. */
private val availableInterests = listOf(
    "Musik", "Sport", "Kunst", "Food", "Tech", "Outdoor", "Theater", "Kino", "Gaming", "Literatur",
)

/**
 * Einstellungen des Benutzers: Profilfelder (Name/E-Mail/Passwort), Interessen,
 * Benachrichtigungs-Schalter (rein lokal, ohne Backend-Persistenz) sowie
 * Darstellungsoptionen (Dark Mode, Hoher Kontrast) über [ThemeState].
 */
@Composable
fun ProfileSettingsScreen(
    navController: NavHostController,
    apiService: ApiService,
    authRepository: AuthRepository,
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var selectedInterests by remember { mutableStateOf<List<String>>(emptyList()) }
    var error by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    // Benachrichtigungs-Einstellungen werden lokal in DataStore persistiert (kein Backend-Endpunkt),
    // damit die Schalter ihren Zustand über App-Neustarts hinweg behalten.
    val eventReminders by ThemeState.eventReminders.collectAsState()
    val newEventsNotifications by ThemeState.newEventsNotifications.collectAsState()
    var showPasswordDialog by remember { mutableStateOf(false) }
    var currentPw by remember { mutableStateOf("") }
    var newPw by remember { mutableStateOf("") }
    var confirmPw by remember { mutableStateOf("") }
    var pwError by remember { mutableStateOf("") }
    var pwSaving by remember { mutableStateOf(false) }
    var pwSuccess by remember { mutableStateOf(false) }
    val darkModeOverride by ThemeState.darkModeOverride.collectAsState()
    val darkMode = darkModeOverride ?: isSystemInDarkTheme()
    val highContrast by ThemeState.highContrast.collectAsState()
    val scope = rememberCoroutineScope()

    // Vorbefüllen der Formularfelder mit dem aktuellen Profil beim ersten Öffnen des Screens.
    LaunchedEffect(Unit) {
        runCatching { apiService.getMyProfile() }
            .onSuccess { profile ->
                username = profile.username
                email = profile.email
                selectedInterests = profile.interests
            }
            .onFailure { error = it.toAuthErrorMessage("Profil konnte nicht geladen werden") }
    }

    /** Fügt ein Interesse hinzu oder entfernt es aus der aktuellen Auswahl. */
    fun toggleInterest(interest: String) {
        selectedInterests = if (selectedInterests.contains(interest)) {
            selectedInterests - interest
        } else {
            selectedInterests + interest
        }
    }

    /** Validiert die Passwort-Eingaben lokal und ruft anschließend den Passwort-Ändern-Endpunkt auf. */
    fun handleChangePassword() {
        pwError = ""
        if (currentPw.isBlank()) {
            pwError = "Bitte aktuelles Passwort eingeben"
            return
        }
        if (newPw.length < 6) {
            pwError = "Passwort muss mindestens 6 Zeichen lang sein"
            return
        }
        if (newPw != confirmPw) {
            pwError = "Passwörter stimmen nicht überein"
            return
        }
        pwSaving = true
        scope.launch {
            try {
                apiService.changePassword(ChangePasswordRequest(currentPw, newPw))
                showPasswordDialog = false
                currentPw = ""
                newPw = ""
                confirmPw = ""
                pwSuccess = true
            } catch (e: Exception) {
                pwError = e.toAuthErrorMessage("Passwort konnte nicht geändert werden")
            } finally {
                pwSaving = false
            }
        }
    }

    /** Speichert Username/E-Mail/Interessen, aktualisiert die lokal zwischengespeicherten Benutzerdaten und kehrt zum Profil zurück. */
    fun handleSave() {
        error = ""
        saving = true
        scope.launch {
            try {
                val updated = apiService.updateProfile(
                    UpdateProfileRequest(
                        username = username,
                        email = email,
                        interests = selectedInterests,
                    ),
                )
                authRepository.updateUser(updated)
                navController.popBackStack(Routes.PROFILE, inclusive = false)
            } catch (e: Exception) {
                error = e.toAuthErrorMessage("Speichern fehlgeschlagen")
            } finally {
                saving = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(text = "Einstellungen") },
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
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Profil bearbeiten", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(text = "Benutzername") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(text = "E-Mail") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                OutlinedButton(
                    onClick = {
                        pwSuccess = false
                        pwError = ""
                        showPasswordDialog = true
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Text(text = "Passwort ändern")
                }
                if (pwSuccess) {
                    Text(
                        text = "Passwort erfolgreich geändert",
                        color = MeetNGoColors.BrandTeal,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Interessen verwalten", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableInterests.forEach { interest ->
                        FilterChip(
                            selected = selectedInterests.contains(interest),
                            onClick = { toggleInterest(interest) },
                            label = { Text(text = interest) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MeetNGoColors.BrandTeal,
                                selectedLabelColor = Color.White,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedInterests.contains(interest),
                                borderColor = MaterialTheme.colorScheme.outline,
                                selectedBorderColor = MeetNGoColors.BrandTeal,
                            ),
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Benachrichtigungen", style = MaterialTheme.typography.titleMedium)
                SettingRow(
                    title = "Event-Erinnerungen",
                    subtitle = "Benachrichtigungen für bevorstehende Events",
                    checked = eventReminders,
                    onCheckedChange = { ThemeState.eventReminders.value = it },
                )
                SettingRow(
                    title = "Neue Events",
                    subtitle = "Events in deiner Nähe",
                    checked = newEventsNotifications,
                    onCheckedChange = { ThemeState.newEventsNotifications.value = it },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Darstellung", style = MaterialTheme.typography.titleMedium)
                SettingRow(
                    title = "Dark Mode",
                    subtitle = "Dunkles Farbschema",
                    checked = darkMode,
                    onCheckedChange = { ThemeState.darkModeOverride.value = it },
                )
                SettingRow(
                    title = "Hoher Kontrast",
                    subtitle = "Bessere Lesbarkeit",
                    checked = highContrast,
                    onCheckedChange = { ThemeState.highContrast.value = it },
                )
            }

            if (error.isNotBlank()) {
                Text(text = error, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = { handleSave() },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MeetNGoColors.BrandTeal),
            ) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                }
                Text(text = if (saving) "Speichern..." else "Speichern")
            }
        }
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { if (!pwSaving) showPasswordDialog = false },
            title = { Text("Passwort ändern") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currentPw,
                        onValueChange = { currentPw = it },
                        label = { Text("Aktuelles Passwort") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = newPw,
                        onValueChange = { newPw = it },
                        label = { Text("Neues Passwort") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = confirmPw,
                        onValueChange = { confirmPw = it },
                        label = { Text("Neues Passwort bestätigen") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (pwError.isNotBlank()) {
                        Text(
                            pwError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { handleChangePassword() }, enabled = !pwSaving) {
                    Text(if (pwSaving) "Speichern..." else "Ändern")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }, enabled = !pwSaving) {
                    Text("Abbrechen")
                }
            },
        )
    }
}

/** Einstellungszeile mit Titel, Untertitel und einem Switch zum Ein-/Ausschalten der jeweiligen Option. */
@Composable
private fun SettingRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
