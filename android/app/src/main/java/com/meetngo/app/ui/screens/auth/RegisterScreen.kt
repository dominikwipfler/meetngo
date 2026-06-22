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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.meetngo.app.data.model.RegisterRequest
import com.meetngo.app.data.repository.AuthRepository
import com.meetngo.app.ui.navigation.Routes
import com.meetngo.app.ui.theme.MeetNGoColors
import kotlinx.coroutines.launch

/**
 * Registrierungs-Bildschirm: Benutzername, E-Mail, Passwort und
 * Passwort-Bestätigung. Validiert die Eingaben lokal, bevor der
 * Registrierungs-Endpunkt aufgerufen wird; bei Erfolg wird der Benutzer
 * sofort eingeloggt und zur Kartenansicht weitergeleitet.
 */
@Composable
fun RegisterScreen(
    navController: NavHostController,
    authRepository: AuthRepository,
    apiService: ApiService,
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    /** Validiert alle Formularfelder lokal, bevor überhaupt ein Netzwerk-Request ausgelöst wird. */
    fun handleRegister() {
        error = ""
        if (username.isBlank() || email.isBlank() || password.isBlank() || passwordConfirm.isBlank()) {
            error = "Alle Felder sind erforderlich"
            return
        }
        if (password != passwordConfirm) {
            error = "Passwörter stimmen nicht überein"
            return
        }
        if (password.length < 6) {
            error = "Passwort muss mindestens 6 Zeichen lang sein"
            return
        }
        loading = true
        scope.launch {
            try {
                val res = apiService.register(RegisterRequest(username, email, password))
                // Registrierung loggt direkt ein (Backend liefert bereits ein gültiges Token zurück).
                authRepository.login(res.token, res.user)
                navController.navigate(Routes.MAP) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            } catch (e: Exception) {
                error = e.toAuthErrorMessage("Registrierung fehlgeschlagen")
            } finally {
                loading = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Zurück-Button navigiert zum vorherigen Screen (i. d. R. Login).
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.padding(8.dp),
        ) {
            Icon(
                Icons.Filled.ArrowBack,
                contentDescription = "Zurück",
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .shadow(12.dp, CircleShape)
                    .background(
                        Brush.linearGradient(listOf(MeetNGoColors.BrandTeal, MeetNGoColors.BrandCoral)),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "M&G", color = Color.White, style = MaterialTheme.typography.titleMedium)
            }

            Text(
                text = "Konto erstellen",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = "Werde Teil der MeetNGo-Community",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            // Eingabefelder: Benutzername, E-Mail, Passwort, Passwort-Bestätigung (in dieser Reihenfolge).
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Benutzername", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    placeholder = { Text("max.mustermann") },
                    singleLine = true,
                    colors = filledFieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("E-Mail", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("max@example.com") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = filledFieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Passwort", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("Mindestens 6 Zeichen") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = filledFieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Passwort bestätigen", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = passwordConfirm,
                    onValueChange = { passwordConfirm = it },
                    placeholder = { Text("••••••••") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { handleRegister() }),
                    colors = filledFieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Fehlertext: lokale Validierungsfehler oder Backend-Fehlermeldung.
            if (error.isNotBlank()) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            // Submit-Button: zeigt während des Requests einen Lade-Indikator statt des Labels.
            Button(
                onClick = { handleRegister() },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MeetNGoColors.BrandCoral),
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Konto erstellen")
                }
            }

            Row(modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    text = "Bereits ein Konto? ",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Einloggen",
                    color = MeetNGoColors.BrandTeal,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable { navController.navigate(Routes.LOGIN) },
                )
            }
        }
    }
}
