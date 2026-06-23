package com.meetngo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.meetngo.app.data.api.ApiClient
import com.meetngo.app.data.api.ApiService
import com.meetngo.app.data.repository.AuthRepository
import com.meetngo.app.ui.navigation.BottomNavBar
import com.meetngo.app.ui.navigation.Routes
import com.meetngo.app.ui.navigation.bottomNavRoutes
import com.meetngo.app.ui.screens.auth.LoginScreen
import com.meetngo.app.ui.screens.auth.RegisterScreen
import com.meetngo.app.ui.screens.createevent.CreateEventScreen
import com.meetngo.app.ui.screens.eventdetail.EventDetailScreen
import com.meetngo.app.ui.screens.map.MapScreen
import com.meetngo.app.ui.screens.organizer.OrganizerDashboardScreen
import com.meetngo.app.ui.screens.profile.ProfileScreen
import com.meetngo.app.ui.screens.profile.ProfileSettingsScreen
import com.meetngo.app.ui.screens.scanner.ScannerScreen
import com.meetngo.app.ui.screens.search.SearchScreen
import com.meetngo.app.ui.screens.tickets.TicketsScreen
import com.meetngo.app.ui.theme.MeetNGoTheme
import com.meetngo.app.ui.theme.ThemeState
import kotlinx.coroutines.runBlocking

/**
 * App-wide singletons: one AuthRepository (DataStore-backed) and one ApiService
 * built on top of it. Screen modules receive these through the NavHost callbacks
 * below instead of re-creating their own — keeps the auth token and base URL
 * consistent everywhere.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Lässt Inhalte unter die System-Statusleiste/Navigationsleiste zeichnen
        // (transparente Balken), damit das App-Theme die Farbgebung übernimmt.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )

        val authRepository = AuthRepository(applicationContext)
        // Blockiert kurz beim App-Start, damit ein vorhandenes Token sofort verfügbar
        // ist, bevor die erste Compose-Oberfläche (und damit mögliche API-Calls) startet.
        runBlocking { authRepository.restoreSession() }
        val apiService = ApiClient.create(authRepository)

        // Load persisted appearance prefs before first composition (no theme flicker).
        ThemeState.init(applicationContext)

        setContent {
            // Manuelle Dark-Mode-Übersteuerung hat Vorrang vor der System-Einstellung.
            val darkOverride by ThemeState.darkModeOverride.collectAsState()
            val isDark = darkOverride ?: isSystemInDarkTheme()
            val isHighContrast by ThemeState.highContrast.collectAsState()
            // Passt die Icon-Farbe der System-Statusleiste/Navigationsleiste an das aktuelle Theme an.
            SideEffect {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.isAppearanceLightStatusBars = !isDark
                controller.isAppearanceLightNavigationBars = !isDark
            }
            MeetNGoTheme(darkTheme = isDark, highContrast = isHighContrast) {
                Surface {
                    val navController = rememberNavController()
                    val isAuthenticated by authRepository.isAuthenticatedFlow.collectAsState(initial = false)
                    val currentEntry by navController.currentBackStackEntryAsState()
                    // Untere Navigationsleiste nur auf den Haupt-Tabs anzeigen, nicht z. B. bei Login/Detailansichten.
                    val showBottomBar = currentEntry?.destination?.hierarchy?.any {
                        it.route in bottomNavRoutes
                    } == true

                    Scaffold(
                        bottomBar = { if (showBottomBar) BottomNavBar(navController) },
                    ) { padding ->
                        // Die Kartenansicht soll randlos bis unter die Statusleiste reichen
                        // (eigene Statusleisten-Aussparung übernimmt MapScreen selbst für sein
                        // Suchfeld), alle anderen Screens behalten das normale obere Padding.
                        val isMapScreen = currentEntry?.destination?.hierarchy?.any {
                            it.route == Routes.MAP
                        } == true
                        val contentPadding = if (isMapScreen) {
                            PaddingValues(
                                start = padding.calculateStartPadding(LocalLayoutDirection.current),
                                top = 0.dp,
                                end = padding.calculateEndPadding(LocalLayoutDirection.current),
                                bottom = padding.calculateBottomPadding(),
                            )
                        } else {
                            padding
                        }
                        Box(Modifier.padding(contentPadding)) {
                            AppNavHost(
                                navController = navController,
                                authRepository = authRepository,
                                apiService = apiService,
                                // Eingeloggte Benutzer starten direkt auf der Kartenansicht, sonst beim Login.
                                startDestination = if (isAuthenticated) Routes.MAP else Routes.LOGIN,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Central navigation graph: maps every [Routes] entry to its screen composable
 * and threads the shared [authRepository] and [apiService] through to each one.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    authRepository: AuthRepository,
    apiService: ApiService,
    startDestination: String,
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            LoginScreen(navController = navController, authRepository = authRepository, apiService = apiService)
        }
        composable(Routes.REGISTER) {
            RegisterScreen(navController = navController, authRepository = authRepository, apiService = apiService)
        }
        composable(Routes.MAP) {
            MapScreen(navController = navController, apiService = apiService)
        }
        composable(Routes.SEARCH) {
            SearchScreen(navController = navController, apiService = apiService)
        }
        composable(Routes.TICKETS) {
            TicketsScreen(navController = navController, apiService = apiService)
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                navController = navController,
                apiService = apiService,
                authRepository = authRepository,
            )
        }
        composable(Routes.PROFILE_SETTINGS) {
            ProfileSettingsScreen(
                navController = navController,
                apiService = apiService,
                authRepository = authRepository,
            )
        }
        composable(Routes.EVENT_DETAIL) { backStackEntry ->
            // eventId kommt als String-Navigationsargument; bei fehlerhafter/fehlender ID wird nichts angezeigt.
            val eventId = backStackEntry.arguments?.getString("eventId")?.toIntOrNull()
            if (eventId != null) {
                EventDetailScreen(
                    navController = navController,
                    apiService = apiService,
                    eventId = eventId,
                )
            } else {
                Box(Modifier)
            }
        }
        composable(Routes.CREATE_EVENT) {
            CreateEventScreen(
                navController = navController,
                apiService = apiService,
            )
        }
        composable(Routes.ORGANIZER_DASHBOARD) {
            OrganizerDashboardScreen(
                navController = navController,
                apiService = apiService,
                authRepository = authRepository,
            )
        }
        composable(Routes.SCANNER) {
            ScannerScreen(navController = navController, apiService = apiService)
        }
    }
}
