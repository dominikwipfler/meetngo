@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.meetngo.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meetngo.app.ui.theme.MeetNGoColors
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

/** Verknüpft den für die API benötigten Sortier-Wert mit Anzeigelabel und passendem Icon. */
data class SortOption(val value: String, val label: String, val icon: ImageVector)

val SORT_OPTIONS = listOf(
    SortOption("date", "Datum", Icons.Filled.CalendarToday),
    SortOption("name", "Name", Icons.Filled.SortByAlpha),
    SortOption("attendees", "Beliebtheit", Icons.Filled.Whatshot),
    SortOption("price", "Preis", Icons.Filled.AttachMoney),
)

/** Obergrenze des Maximalpreis-Sliders; am rechten Anschlag gilt "kein Limit". */
const val MAX_PRICE_CAP = 200

/** Datums-Schnellfilter mit ihrem internen Schlüssel und dem Anzeigelabel. */
data class DatePreset(val key: String, val label: String)

val DATE_PRESETS = listOf(
    DatePreset("", "Alle"),
    DatePreset("today", "Heute"),
    DatePreset("weekend", "Wochenende"),
    DatePreset("week", "Diese Woche"),
)

/**
 * Sortierung, Reihenfolge, Preis- und Datumsfilter für Veranstaltungslisten — gemeinsam
 * genutzt von Karte und Suche, damit beide dieselbe Filterlogik anbieten.
 */
data class EventFilters(
    val sort: String = "date",
    val order: String = "asc",
    val priceFilter: String = "", // "", "free", "paid"
    val maxPrice: Int = 0, // 0 = kein Limit
    val datePreset: String = "", // "", "today", "weekend", "week", "custom"
    val customDateMillis: Long? = null,
) {
    // Zählt nur "von der Standardeinstellung abweichende" Filter, damit das Badge nicht ständig zählt.
    val activeCount: Int
        get() = listOf(
            priceFilter.isNotEmpty(),
            sort != "date",
            order != "asc",
            maxPrice > 0,
            datePreset.isNotEmpty(),
        ).count { it }
}

private val ISO_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

/**
 * Löst den gewählten Datums-Filter in ein konkretes ISO-Zeitfenster (von/bis) auf, das ans
 * Backend geschickt werden kann. `null`/`null` bedeutet "kein Datumsfilter".
 */
fun EventFilters.resolveDateRange(): Pair<String?, String?> {
    fun startOf(date: LocalDate) = date.atStartOfDay().format(ISO_DATE_TIME)
    fun endOf(date: LocalDate) = date.atTime(23, 59, 59).format(ISO_DATE_TIME)

    val today = LocalDate.now()
    return when (datePreset) {
        "today" -> startOf(today) to endOf(today)
        "weekend" -> {
            // Aktuelles Wochenende (Sa–So); ist heute schon Sonntag, zählt der heutige Tag dazu.
            val saturday = if (today.dayOfWeek == DayOfWeek.SUNDAY) {
                today.minusDays(1)
            } else {
                today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
            }
            startOf(saturday) to endOf(saturday.plusDays(1))
        }
        "week" -> startOf(today) to endOf(today.plusDays(7))
        "custom" -> customDateMillis?.let {
            val date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
            startOf(date) to endOf(date)
        } ?: (null to null)
        else -> null to null
    }
}

/** Filter-Button mit Badge, der die Anzahl aktiver (vom Standard abweichender) Filter anzeigt. */
@Composable
fun FilterButtonWithBadge(activeCount: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    BadgedBox(
        badge = {
            if (activeCount > 0) {
                Badge(containerColor = MeetNGoColors.BrandCoral, contentColor = Color.White) { Text("$activeCount") }
            }
        },
        modifier = modifier,
    ) {
        FilledTonalButton(
            onClick = onClick,
            shape = RoundedCornerShape(50),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 10.dp),
        ) {
            Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(text = "  Filter", fontWeight = FontWeight.Medium)
        }
    }
}

/** Bottom-Sheet mit Sortierung, Reihenfolge, Datums- und Preisfilter. */
@Composable
fun EventFilterSheet(filters: EventFilters, onFiltersChange: (EventFilters) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    var showDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
        ) {
            // Kopfzeile: Titel links, "Zurücksetzen" rechts (nur sichtbar, wenn überhaupt etwas abweicht).
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Filter & Sortierung",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                if (filters.activeCount > 0) {
                    TextButton(onClick = { onFiltersChange(EventFilters()) }) {
                        Text("Zurücksetzen", color = MeetNGoColors.BrandCoral)
                    }
                }
            }

            // --- Datum ---
            FilterSectionLabel("Wann")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DATE_PRESETS.forEach { preset ->
                    val selected = preset.key == filters.datePreset
                    FilterChip(
                        selected = selected,
                        onClick = { onFiltersChange(filters.copy(datePreset = preset.key, customDateMillis = null)) },
                        label = { Text(preset.label) },
                        shape = RoundedCornerShape(50),
                        colors = brandChipColors(),
                        border = brandChipBorder(selected),
                    )
                }
                // Eigenes Datum: öffnet einen Date-Picker; das gewählte Datum erscheint im Chip-Label.
                val customSelected = filters.datePreset == "custom"
                val customLabel = filters.customDateMillis
                    ?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().format(GERMAN_DAY) }
                    ?: "Datum wählen"
                FilterChip(
                    selected = customSelected,
                    onClick = { showDatePicker = true },
                    label = { Text(customLabel) },
                    leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    shape = RoundedCornerShape(50),
                    colors = brandChipColors(),
                    border = brandChipBorder(customSelected),
                )
            }

            // --- Sortierung ---
            FilterSectionLabel("Sortieren nach", topPadding = 24.dp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SORT_OPTIONS.forEach { option ->
                    val selected = option.value == filters.sort
                    FilterChip(
                        selected = selected,
                        onClick = { onFiltersChange(filters.copy(sort = option.value)) },
                        label = { Text(option.label) },
                        leadingIcon = { Icon(option.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        shape = RoundedCornerShape(50),
                        colors = brandChipColors(),
                        border = brandChipBorder(selected),
                    )
                }
            }

            FilterSectionLabel("Reihenfolge", topPadding = 24.dp)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val ascending = filters.order == "asc"
                SegmentedButton(
                    selected = ascending,
                    onClick = { onFiltersChange(filters.copy(order = "asc")) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = { SegmentedButtonDefaults.Icon(active = ascending) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, modifier = Modifier.size(18.dp)) } },
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MeetNGoColors.BrandTeal.copy(alpha = 0.15f),
                        activeContentColor = MeetNGoColors.BrandTeal,
                    ),
                ) { Text("Aufsteigend") }
                SegmentedButton(
                    selected = !ascending,
                    onClick = { onFiltersChange(filters.copy(order = "desc")) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = { SegmentedButtonDefaults.Icon(active = !ascending) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp)) } },
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MeetNGoColors.BrandTeal.copy(alpha = 0.15f),
                        activeContentColor = MeetNGoColors.BrandTeal,
                    ),
                ) { Text("Absteigend") }
            }

            // --- Preis ---
            FilterSectionLabel("Preis", topPadding = 24.dp)
            val priceOptions = listOf(
                Triple("", "Alle", null),
                Triple("free", "Kostenlos", Icons.Filled.MoneyOff),
                Triple("paid", "Kostenpflichtig", Icons.Filled.AttachMoney),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                priceOptions.forEach { (value, label, icon) ->
                    val selected = value == filters.priceFilter
                    FilterChip(
                        selected = selected,
                        onClick = { onFiltersChange(filters.copy(priceFilter = value)) },
                        label = { Text(label) },
                        leadingIcon = icon?.let { { Icon(it, contentDescription = null, modifier = Modifier.size(18.dp)) } },
                        shape = RoundedCornerShape(50),
                        colors = brandChipColors(),
                        border = brandChipBorder(selected),
                    )
                }
            }

            // Maximalpreis-Slider; am rechten Anschlag (MAX_PRICE_CAP) gilt "kein Limit" (maxPrice = 0).
            // Bei "Kostenlos" ist der Slider ausgegraut, da der Maximalpreis dort keine Rolle spielt.
            val priceLimitEnabled = filters.priceFilter != "free"
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Maximaler Preis",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (!priceLimitEnabled || filters.maxPrice == 0) "Egal" else "bis ${filters.maxPrice} €",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (priceLimitEnabled && filters.maxPrice > 0) MeetNGoColors.BrandTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Slider(
                value = if (filters.maxPrice == 0) MAX_PRICE_CAP.toFloat() else filters.maxPrice.toFloat(),
                onValueChange = { raw ->
                    val v = raw.roundToInt()
                    onFiltersChange(filters.copy(maxPrice = if (v >= MAX_PRICE_CAP) 0 else v))
                },
                valueRange = 0f..MAX_PRICE_CAP.toFloat(),
                steps = (MAX_PRICE_CAP / 5) - 1,
                enabled = priceLimitEnabled,
                colors = SliderDefaults.colors(
                    thumbColor = MeetNGoColors.BrandTeal,
                    activeTrackColor = MeetNGoColors.BrandTeal,
                ),
            )

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MeetNGoColors.BrandTeal),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 20.dp),
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Text("  Anwenden", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showDatePicker) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = filters.customDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val picked = dateState.selectedDateMillis
                    if (picked != null) {
                        onFiltersChange(filters.copy(datePreset = "custom", customDateMillis = picked))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Abbrechen") }
            },
        ) {
            DatePicker(state = dateState)
        }
    }
}

private val GERMAN_DAY = DateTimeFormatter.ofPattern("dd.MM.")

/** Abschnitts-Überschrift im Filter-Sheet. */
@Composable
private fun FilterSectionLabel(text: String, topPadding: androidx.compose.ui.unit.Dp = 12.dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = topPadding, bottom = 12.dp),
    )
}

/** Einheitliche Markenfarben für die ausgewählten Filter-Chips im Sheet. */
@Composable
private fun brandChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MeetNGoColors.BrandTeal,
    selectedLabelColor = Color.White,
    selectedLeadingIconColor = Color.White,
)

/** Einheitlicher Rahmen für die Filter-Chips im Sheet. */
@Composable
private fun brandChipBorder(selected: Boolean) = FilterChipDefaults.filterChipBorder(
    enabled = true,
    selected = selected,
    borderColor = MaterialTheme.colorScheme.outline,
    selectedBorderColor = MeetNGoColors.BrandTeal,
)
