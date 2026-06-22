package com.meetngo.app.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Kurzformat, z. B. "21.06.2026". */
private val GERMAN_SHORT = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMANY)
/** Mittleres Format ohne Uhrzeit, z. B. "21. Juni 2026". */
private val GERMAN_MEDIUM = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMANY)
/** Langes Format mit Uhrzeit, z. B. "21. Juni 2026, 19:30" (Suffix "Uhr" wird separat angehängt). */
private val GERMAN_LONG = DateTimeFormatter.ofPattern("d. MMMM yyyy, HH:mm", Locale.GERMANY)

/**
 * Formatiert ein ISO-Datum kompakt im deutschen Format (TT.MM.JJJJ).
 * Bei einem nicht parsbaren [dateStr] wird der Rohwert unverändert zurückgegeben, statt zu crashen.
 */
fun formatDateShort(dateStr: String): String =
    runCatching { LocalDateTime.parse(dateStr).format(GERMAN_SHORT) }.getOrDefault(dateStr)

/**
 * Formatiert ein ISO-Datum ausführlich ohne Uhrzeit (Tag, ausgeschriebener Monat, Jahr).
 */
fun formatDateMedium(dateStr: String): String =
    runCatching { LocalDateTime.parse(dateStr).format(GERMAN_MEDIUM) }.getOrDefault(dateStr)

/**
 * Formatiert ein ISO-Datum ausführlich inklusive Uhrzeit und dem deutschen "Uhr"-Suffix.
 */
fun formatDateLong(dateStr: String): String =
    runCatching { LocalDateTime.parse(dateStr).format(GERMAN_LONG) + " Uhr" }.getOrDefault(dateStr)
