package com.meetngo.app.ui.screens.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthErrorUtilsTest {

    @Test
    fun `parseErrorMessage extrahiert die error-Nachricht aus validem JSON`() {
        assertEquals(
            "E-Mail bereits vergeben",
            parseErrorMessage("""{"error":"E-Mail bereits vergeben"}"""),
        )
    }

    @Test
    fun `parseErrorMessage gibt null zurueck wenn der Body null ist`() {
        assertNull(parseErrorMessage(null))
    }

    @Test
    fun `parseErrorMessage gibt null zurueck wenn der Body leer ist`() {
        assertNull(parseErrorMessage(""))
    }

    @Test
    fun `parseErrorMessage gibt null zurueck bei nicht-JSON Body`() {
        assertNull(parseErrorMessage("<html>502 Bad Gateway</html>"))
    }

    @Test
    fun `parseErrorMessage gibt null zurueck wenn das error-Feld leer ist`() {
        assertNull(parseErrorMessage("""{"error":""}"""))
    }

    @Test
    fun `toAuthErrorMessage gibt den Fallback zurueck wenn es keine HttpException ist`() {
        val result = RuntimeException("Netzwerkfehler").toAuthErrorMessage("Etwas ist schiefgelaufen")
        assertEquals("Etwas ist schiefgelaufen", result)
    }
}
