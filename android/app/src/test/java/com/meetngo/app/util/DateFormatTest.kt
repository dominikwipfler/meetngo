package com.meetngo.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DateFormatTest {

    private val sample = "2026-06-21T19:30:00"

    @Test
    fun `formatDateShort gibt deutsches Kurzformat zurueck`() {
        assertEquals("21.06.2026", formatDateShort(sample))
    }

    @Test
    fun `formatDateMedium gibt ausgeschriebenen Monat ohne Uhrzeit zurueck`() {
        assertEquals("21. Juni 2026", formatDateMedium(sample))
    }

    @Test
    fun `formatDateLong gibt ausgeschriebenes Datum mit Uhrzeit und Uhr-Suffix zurueck`() {
        assertEquals("21. Juni 2026, 19:30 Uhr", formatDateLong(sample))
    }

    @Test
    fun `formatDateShort gibt Rohwert zurueck wenn das Datum nicht parsbar ist`() {
        assertEquals("nicht-ein-datum", formatDateShort("nicht-ein-datum"))
    }

    @Test
    fun `formatDateLong gibt Rohwert zurueck wenn das Datum nicht parsbar ist`() {
        assertEquals("", formatDateLong(""))
    }
}
