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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.meetngo.app.data.api.ApiService
import com.meetngo.app.data.repository.AuthRepository
import com.meetngo.app.ui.navigation.Routes
import com.meetngo.app.ui.theme.MeetNGoColors

/**
 * Login-Bildschirm: Logo/Branding, E-Mail- und Passwortfeld sowie ein Link
 * zur Registrierung. Der UI-Zustand und der Login-Aufruf liegen im
 * [LoginViewModel]; bei erfolgreichem Login wird zur Kartenansicht navigiert.
 */
@Composable
fun LoginScreen(
    navController: NavHostController,
    authRepository: AuthRepository,
    apiService: ApiService,
) {
    val viewModel: LoginViewModel = viewModel(
        factory = viewModelFactory {
            initializer { LoginViewModel(authRepository, apiService) }
        },
    )
    val uiState by viewModel.uiState.collectAsState()

    // Navigiert erst, nachdem der ViewModel-State auf "eingeloggt" gewechselt hat,
    // statt direkt im Login-Callback — damit die Navigation nicht mehrfach feuert.
    LaunchedEffect(uiState.loggedIn) {
        if (uiState.loggedIn) {
            navController.navigate(Routes.MAP) {
                // Entfernt den Login-Screen vollständig aus dem Backstack, damit der Zurück-Button
                // nach erfolgreichem Login nicht wieder dorthin zurückführt.
                popUpTo(Routes.LOGIN) { inclusive = true }
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
                    value = uiState.email,
                    onValueChange = { viewModel.onEmailChange(it) },
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
                    value = uiState.password,
                    onValueChange = { viewModel.onPasswordChange(it) },
                    placeholder = { Text("••••••••") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { viewModel.login() }),
                    colors = filledFieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Fehlertext wird nur angezeigt, wenn Validierung oder Backend-Aufruf fehlgeschlagen sind.
            if (uiState.error.isNotBlank()) {
                Text(text = uiState.error, color = MaterialTheme.colorScheme.error)
            }

            // Login-Button: zeigt während des Requests einen Lade-Indikator statt des Labels.
            Button(
                onClick = { viewModel.login() },
                enabled = !uiState.loading,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MeetNGoColors.BrandCoral,
                ),
            ) {
                if (uiState.loading) {
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
