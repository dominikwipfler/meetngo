const express = require("express");
const db = require("../database");
const { authMiddleware } = require("../middleware/auth");

const router = express.Router();

function ticketWithEvent(ticketId) {
  return db
    .prepare(
      `
      SELECT
        tickets.id, tickets.event_id, tickets.user_id, tickets.status, tickets.created_at,
        events.name AS event_name, events.date AS event_date, events.location AS event_location,
        events.image_path AS event_image_path, events.price AS event_price
      FROM tickets
      JOIN events ON events.id = tickets.event_id
      WHERE tickets.id = ?
    `,
    )
    .get(ticketId);
}

// GET /api/tickets — the authenticated user's own tickets, newest event first
router.get("/", authMiddleware, (req, res) => {
  const tickets = db
    .prepare(
      `
      SELECT
        tickets.id, tickets.event_id, tickets.user_id, tickets.status, tickets.created_at,
        events.name AS event_name, events.date AS event_date, events.location AS event_location,
        events.image_path AS event_image_path, events.price AS event_price
      FROM tickets
      JOIN events ON events.id = tickets.event_id
      WHERE tickets.user_id = ?
      ORDER BY events.date ASC
    `,
    )
    .all(req.user.userId);

  res.json(tickets);
});

// POST /api/tickets { eventId } — "buys" a ticket for the authenticated user
router.post("/", authMiddleware, (req, res) => {
  const eventId = parseInt(req.body.eventId, 10);
  if (!eventId) {
    return res.status(400).json({ error: "eventId ist erforderlich" });
  }

  const event = db.prepare("SELECT * FROM events WHERE id = ?").get(eventId);
  if (!event) {
    return res.status(404).json({ error: "Event nicht gefunden" });
  }
  if (event.capacity != null && event.attendees >= event.capacity) {
    return res.status(409).json({ error: "Event ist ausgebucht" });
  }

  const insertTicket = db.prepare(
    "INSERT INTO tickets (event_id, user_id, status) VALUES (?, ?, 'active')",
  );
  const incrementAttendees = db.prepare("UPDATE events SET attendees = attendees + 1 WHERE id = ?");

  const result = db.transaction(() => {
    const ticket = insertTicket.run(eventId, req.user.userId);
    incrementAttendees.run(eventId);
    return ticket;
  })();

  res.status(201).json(ticketWithEvent(result.lastInsertRowid));
});

module.exports = router;
