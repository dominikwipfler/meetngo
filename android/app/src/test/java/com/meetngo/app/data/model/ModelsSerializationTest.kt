package com.meetngo.app.data.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Die @SerializedName-Annotationen in Models.kt mappen die snake_case-Felder des
 * Backends (siehe backend/database.js) auf camelCase-Properties. Ein Tippfehler dort
 * würde beim Kompilieren nicht auffallen, sondern erst zur Laufzeit zu stillschweigend
 * leeren/falschen Werten führen — diese Tests fangen genau das ab.
 */
class ModelsSerializationTest {

    private val gson = Gson()

    @Test
    fun `Event JSON vom Backend wird korrekt auf camelCase-Felder gemappt`() {
        val json = """
            {
              "id": 1,
              "name": "Sommerfest",
              "description": "Open Air",
              "category": "Musik",
              "location": "Karlsruhe",
              "lat": 49.0,
              "lng": 8.4,
              "date": "2026-07-12T18:00:00",
              "price": "12,50 €",
              "price_value": 12.5,
              "capacity": 100,
              "attendees": 42,
              "image_path": "/uploads/sommerfest.jpg",
              "organizer_id": 7,
              "organizer": "Stadt Karlsruhe",
              "featured": 1,
              "active": 1
            }
        """.trimIndent()

        val event = gson.fromJson(json, Event::class.java)

        assertEquals(12.5, event.priceValue, 0.0)
        assertEquals("/uploads/sommerfest.jpg", event.imagePath)
        assertEquals(7, event.organizerId)
        assertEquals(42, event.attendees)
    }

    @Test
    fun `Ticket JSON vom Backend wird korrekt auf camelCase-Felder gemappt`() {
        val json = """
            {
              "id": 5,
              "event_id": 1,
              "user_id": 3,
              "status": "active",
              "event_name": "Sommerfest",
              "event_date": "2026-07-12T18:00:00",
              "event_location": "Karlsruhe",
              "event_image_path": "/uploads/sommerfest.jpg",
              "event_price": "12,50 €"
            }
        """.trimIndent()

        val ticket = gson.fromJson(json, Ticket::class.java)

        assertEquals(1, ticket.eventId)
        assertEquals(3, ticket.userId)
        assertEquals("Sommerfest", ticket.eventName)
        assertEquals("/uploads/sommerfest.jpg", ticket.eventImagePath)
    }
}
