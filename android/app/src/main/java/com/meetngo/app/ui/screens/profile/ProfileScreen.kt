@file:OptIn(ExperimentalLayoutApi::class)

package com.meetngo.app.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.meetngo.app.data.api.ApiService
import com.meetngo.app.data.api.BASE_URL
import com.meetngo.app.data.model.Event
import com.meetngo.app.data.repository.AuthRepository
import com.meetngo.app.ui.navigation.Routes
import com.meetngo.app.ui.theme.MeetNGoColors
import kotlinx.coroutines.launch

/** Baut aus einem relativen Backend-Pfad eine vollständige Bild-URL; absolute URLs werden unverändert übernommen. */
private fun imageUrl(imagePath: String?): String? {
    if (imagePath.isNullOrBlank()) return null
    if (imagePath.startsWith("http")) return imagePath
    return BASE_URL.removeSuffix("/") + "/" + imagePath.trimStart('/')
}

/**
 * Profil-Übersicht des eingeloggten Benutzers: Avatar, Statistiken
 * (eigene Events, Favoriten, Abonnenten), Interessen und eine Vorschau
 * der eigenen Veranstaltungen mit Link zum vollständigen Dashboard.
 */
@Composable
fun ProfileScreen(
    navController: NavHostController,
    apiService: ApiService,
    authRepository: AuthRepository,
) {
    val user by authRepository.userFlow.collectAsState(initial = null)
    var myEvents by remember { mutableStateOf<List<Event>>(emptyList()) }
    var interests by remember { mutableStateOf<List<String>>(emptyList()) }
    var followers by remember { mutableStateOf(0) }
    var favoritesCount by remember { mutableStateOf(0) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Lädt alle Profil-bezogenen Daten unabhängig voneinander, damit ein einzelner fehlschlagender
    // Request (z. B. Favoriten) nicht die Anzeige der übrigen Daten verhindert.
    LaunchedEffect(user?.id) {
        val uid = user?.id ?: return@LaunchedEffect
        runCatching { apiService.getEvents(sort = "date", order = "asc", organizerId = uid) }
            .onSuccess { events -> myEvents = events }
        runCatching { apiService.getMyProfile() }
            .onSuccess { profile -> interests = profile.interests }
        runCatching { apiService.followStatus(uid) }
            .onSuccess { followers = it.followers }
        runCatching { apiService.getFavorites() }
            .onSuccess { favoritesCount = it.size }
    }

    /** Löscht die lokale Sitzung und navigiert zum Login, wobei der gesamte bisherige Backstack verworfen wird. */
    fun handleLogout() {
        showLogoutDialog = false
        scope.launch {
            authRepository.logout()
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(128.dp)
                .background(Brush.linearGradient(listOf(MeetNGoColors.BrandTeal, MeetNGoColors.BrandCoral))),
        ) {
            IconButton(
                onClick = { navController.navigate(Routes.PROFILE_SETTINGS) },
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            ) {
                Icon(Icons.Filled.Settings, contentDescription = "Einstellungen", tint = Color.White)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-64).dp)
                .padding(horizontal = 16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(4.dp, MaterialTheme.colorScheme.surface),
                    modifier = Modifier.size(96.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(48.dp))
                    }
                }
                Text(
                    text = user?.username ?: "Gast",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    text = "@${(user?.username ?: "gast").lowercase().replace(" ", "")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                user?.email?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    StatItem(value = myEvents.size.toString(), label = "Events")
                    StatItem(value = favoritesCount.toString(), label = "Favoriten")
                    StatItem(value = followers.toString(), label = "Abonnenten")
                }
            }

            Column(modifier = Modifier.padding(top = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Interessen", style = MaterialTheme.typography.titleMedium)
                if (interests.isEmpty()) {
                    Text(
                        text = "Noch keine Interessen ausgewählt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        interests.forEach { interest ->
                            Badge(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary,
                            ) {
                                Text(text = interest, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(top = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "Meine Events", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { navController.navigate(Routes.ORGANIZER_DASHBOARD) }) {
                        Icon(Icons.Filled.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(text = " Dashboard")
                    }
                }

                if (myEvents.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Du hast noch keine Events erstellt.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(
                                onClick = { navController.navigate(Routes.CREATE_EVENT) },
                                colors = ButtonDefaults.buttonColors(containerColor = MeetNGoColors.BrandCoral),
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(text = " Event erstellen")
                            }
                        }
                    }
                } else {
                    // Vorschau-Raster: zeigt maximal 4 eigene Events; vollständige Liste gibt es im Dashboard.
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().height(if (myEvents.take(4).size > 2) 340.dp else 170.dp),
                    ) {
                        items(myEvents.take(4)) { event ->
                            Card(
                                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                                shape = RoundedCornerShape(12.dp),
                                onClick = { navController.navigate(Routes.eventDetail(event.id)) },
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    val img = imageUrl(event.imagePath)
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
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)))),
                                    )
                                    Text(
                                        text = event.name,
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                                    )
                                }
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = { navController.navigate(Routes.ORGANIZER_DASHBOARD) },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    ) {
                        Icon(Icons.Filled.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(text = " Alle Events & Statistiken")
                    }
                }
            }

            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 24.dp).height(48.dp),
            ) {
                Text(text = "Ausloggen", color = MeetNGoColors.Destructive)
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(text = "Bist du sicher?") },
            text = { Text(text = "Möchtest du dich wirklich ausloggen?") },
            confirmButton = {
                TextButton(onClick = { handleLogout() }) {
                    Text(text = "Ja, ausloggen", color = MeetNGoColors.BrandCoral)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text(text = "Nein") }
            },
        )
    }
}

/** Kleine Kennzahl-Anzeige (Wert über Label), z. B. für Events-/Favoriten-/Abonnenten-Zähler. */
@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
