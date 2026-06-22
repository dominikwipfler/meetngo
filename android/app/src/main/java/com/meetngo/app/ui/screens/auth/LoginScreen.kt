package com.meetngo.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.meetngo.app.data.api.ApiService
import com.meetngo.app.data.model.LoginRequest
import com.meetngo.app.data.repository.AuthRepository
import com.meetngo.app.ui.navigation.Routes
import com.meetngo.app.ui.theme.MeetNGoColors
import kotlinx.coroutines.launch

/**
 * Login-Bildschirm: Logo/Branding, E-Mail- und Passwortfeld sowie ein Link
 * zur Registrierung. Bei erfolgreichem Login wird die Sitzung im
 * [authRepository] gespeichert und zur Kartenansicht navigiert.
 */
@Composable
fun LoginScreen(
    navController: NavHostController,
    authRepository: AuthRepository,
    apiService: ApiService,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    /** Validiert die Eingaben, ruft den Login-Endpunkt auf und navigiert bei Erfolg zur Kartenansicht. */
    fun handleLogin() {
        error = ""
        if (email.isBlank() || password.isBlank()) {
            error = "Bitte E-Mail und Passwort eingeben"
            return
        }
        loading = true
        scope.launch {
            try {
                val res = apiService.login(LoginRequest(email, password))
                authRepository.login(res.token, res.user)
                navController.navigate(Routes.MAP) {
                    // Entfernt den Login-Screen vollständig aus dem Backstack, damit der Zurück-Button
                    // nach erfolgreichem Login nicht wieder dorthin zurückführt.
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            } catch (e: Exception) {
                error = e.toAuthErrorMessage("Anmeldung fehlgeschlagen")
            } finally {
                loading = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // App-Logo: kreisförmiger Verlauf von Markenfarbe Teal zu Coral mit "M&G"-Initialen.
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .shadow(12.dp, CircleShape)
                    .background(
                        Brush.linearGradient(listOf(MeetNGoColors.BrandTeal, MeetNGoColors.BrandCoral)),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "M&G", color = Color.White, style = MaterialTheme.typography.titleLarge)
            }

            // App-Name und Untertitel.
            Text(text = "MeetNGo", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "Entdecke Events in deiner Nähe",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // E-Mail-Eingabefeld.
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("E-Mail", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("max@example.com") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    colors = filledFieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Passwort-Eingabefeld; "Fertig" auf der Tastatur löst direkt den Login aus.
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Passwort", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("••••••••") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { handleLogin() }),
                    colors = filledFieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Fehlertext wird nur angezeigt, wenn Validierung oder Backend-Aufruf fehlgeschlagen sind.
            if (error.isNotBlank()) {
                Text(text = error, color = MaterialTheme.colorScheme.error)
            }

            // Login-Button: zeigt während des Requests einen Lade-Indikator statt des Labels.
            Button(
                onClick = { handleLogin() },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MeetNGoColors.BrandCoral,
                ),
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Einloggen")
                }
            }

            // Link zur Registrierung für neue Benutzer.
            Row {
                Text(
                    text = "Noch kein Konto? ",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Registrieren",
                    color = MeetNGoColors.BrandTeal,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable { navController.navigate(Routes.REGISTER) },
                )
            }
        }
    }
}
