package com.meetngo.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.meetngo.app.ui.theme.MeetNGoColors

/** Einzelner Eintrag der unteren Navigationsleiste: Zielroute, sichtbares Label und Icon. */
private data class BottomNavEntry(val route: String, val label: String, val icon: ImageVector)

/** Die vier Haupt-Tabs der App, in der Reihenfolge, wie sie unten angezeigt werden. */
private val bottomNavEntries = listOf(
    BottomNavEntry(Routes.MAP, "Karte", Icons.Filled.Map),
    BottomNavEntry(Routes.SEARCH, "Suche", Icons.Filled.Search),
    BottomNavEntry(Routes.TICKETS, "Tickets", Icons.Filled.ConfirmationNumber),
    BottomNavEntry(Routes.PROFILE, "Profil", Icons.Filled.Person),
)

/** Visible only on the four main tabs. */
val bottomNavRoutes = bottomNavEntries.map { it.route }.toSet()

/**
 * Untere Tab-Navigationsleiste mit den vier Haupt-Bereichen der App.
 * Hervorhebung des aktiven Tabs erfolgt anhand der aktuellen NavController-Destination.
 */
@Composable
fun BottomNavBar(navController: NavHostController) {
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentEntry?.destination

    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        bottomNavEntries.forEach { entry ->
            // hierarchy berücksichtigt auch verschachtelte Graphen, falls eine Route Teil eines Sub-Graphen ist.
            val selected = currentDestination?.hierarchy?.any { it.route == entry.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(entry.route) {
                            // Bewahrt den Scroll-/UI-Zustand der Tabs beim Wechseln (wie bei klassischer Tab-Navigation).
                            popUpTo(Routes.MAP) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(entry.icon, contentDescription = entry.label) },
                label = { Text(entry.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MeetNGoColors.BrandTeal,
                    selectedTextColor = MeetNGoColors.BrandTeal,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = Color.Transparent,
                ),
            )
        }
    }
}
