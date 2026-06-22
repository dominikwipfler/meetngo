package com.meetngo.app.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/**
 * App-weite Darstellungs-Einstellungen (Dark Mode und Hoher Kontrast).
 * Über DataStore persistiert, damit die Wahl App-Neustarts übersteht — [init]
 * einmal aus der MainActivity aufrufen, bevor die Flows gelesen werden.
 */
object ThemeState {
    /** null bedeutet "Systemeinstellung folgen". */
    val darkModeOverride = MutableStateFlow<Boolean?>(null)

    /** Hoher-Kontrast-Modus für bessere Lesbarkeit. */
    val highContrast = MutableStateFlow(false)

    /** Notification preferences. Persisted locally so the toggles remember their state across restarts. */
    val eventReminders = MutableStateFlow(true)
    val newEventsNotifications = MutableStateFlow(true)

    private val darkKey = booleanPreferencesKey("dark_mode_override")
    private val highContrastKey = booleanPreferencesKey("high_contrast")
    private val eventRemindersKey = booleanPreferencesKey("event_reminders")
    private val newEventsKey = booleanPreferencesKey("new_events_notifications")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Loads persisted values, then keeps DataStore in sync with later changes. */
    fun init(context: Context) {
        val appContext = context.applicationContext
        // Synchrones Laden beim Start, damit die erste Compose-Komposition bereits
        // die gespeicherten Einstellungen kennt (kein kurzes Aufblitzen des Standard-Themes).
        val prefs = runBlocking { appContext.settingsDataStore.data.first() }
        darkModeOverride.value = prefs[darkKey]
        highContrast.value = prefs[highContrastKey] ?: false
        eventReminders.value = prefs[eventRemindersKey] ?: true
        newEventsNotifications.value = prefs[newEventsKey] ?: true

        // Ab hier wird jede Änderung der StateFlows automatisch zurück in den DataStore geschrieben.
        scope.launch {
            darkModeOverride.collect { value ->
                appContext.settingsDataStore.edit {
                    // null entfernt den gespeicherten Wert komplett, damit künftig wieder "System folgen" gilt.
                    if (value == null) it.remove(darkKey) else it[darkKey] = value
                }
            }
        }
        scope.launch {
            highContrast.collect { value ->
                appContext.settingsDataStore.edit { it[highContrastKey] = value }
            }
        }
        scope.launch {
            eventReminders.collect { value ->
                appContext.settingsDataStore.edit { it[eventRemindersKey] = value }
            }
        }
        scope.launch {
            newEventsNotifications.collect { value ->
                appContext.settingsDataStore.edit { it[newEventsKey] = value }
            }
        }
    }
}
