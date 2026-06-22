package com.meetngo.app.ui.navigation

/** Zentrale Sammlung aller Navigations-Routen, damit Routennamen nicht als verstreute String-Literale auftauchen. */
object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAP = "map"
    const val SEARCH = "search"
    const val TICKETS = "tickets"
    const val PROFILE = "profile"
    const val PROFILE_SETTINGS = "profile_settings"
    /** Routen-Template mit Platzhalter; zum Navigieren stattdessen [eventDetail] mit konkreter ID verwenden. */
    const val EVENT_DETAIL = "event_detail/{eventId}"
    const val CREATE_EVENT = "create_event"
    const val ORGANIZER_DASHBOARD = "organizer_dashboard"
    const val SCANNER = "scanner"

    /** Baut die konkrete Detail-Route für eine bestimmte Veranstaltung, z. B. für `navController.navigate(...)`. */
    fun eventDetail(eventId: Int) = "event_detail/$eventId"
}

/**
 * Each screen module (auth, map, search, tickets, profile) registers its own
 * composable() routes against this NavHostController inside MainActivity's
 * NavHost — see MainActivity.kt for the wiring point.
 */
