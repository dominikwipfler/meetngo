const express = require("express");
const db = require("../database");
const { authMiddleware } = require("../middleware/auth");

const router = express.Router();

// Tickets are always returned enriched with their event's display fields, so
// the SELECT/JOIN is shared between the single- and list-fetch queries below.
const TICKET_WITH_EVENT_SELECT = `
  SELECT
    tickets.id, tickets.event_id, tickets.user_id, tickets.status, tickets.created_at,
    events.name AS event_name, events.date AS event_date, events.location AS event_location,
    events.image_path AS event_image_path, events.price AS event_price
  FROM tickets
  JOIN events ON events.id = tickets.event_id
`;

function ticketWithEvent(ticketId) {
  return db.prepare(`${TICKET_WITH_EVENT_SELECT} WHERE tickets.id = ?`).get(ticketId);
}

// GET /api/tickets — the authenticated user's own tickets, soonest event first
router.get("/", authMiddleware, (req, res) => {
  const tickets = db
    .prepare(`${TICKET_WITH_EVENT_SELECT} WHERE tickets.user_id = ? ORDER BY events.date ASC`)
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
  // An organizer deactivates an event precisely to stop sales, so reject new
  // ticket purchases for it (it's also hidden from the public browse list).
  if (event.active === 0) {
    return res.status(409).json({ error: "Event ist nicht mehr verfügbar" });
  }
  if (event.capacity != null && event.attendees >= event.capacity) {
    return res.status(409).json({ error: "Event ist ausgebucht" });
  }
  // Ein Nutzer kann pro Event nur ein Ticket besitzen — ein erneuter Kauf würde
  // sonst die attendees-Zahl verfälschen und mehrere QR-Codes für dieselbe
  // Person erzeugen.
  const existing = db
    .prepare("SELECT id FROM tickets WHERE event_id = ? AND user_id = ?")
    .get(eventId, req.user.userId);
  if (existing) {
    return res.status(409).json({ error: "Du hast bereits ein Ticket für dieses Event" });
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

// DELETE /api/tickets/:id — cancels the authenticated user's own ticket and
// frees up one attendee slot on the event (mirrors the POST increment).
router.delete("/:id", authMiddleware, (req, res) => {
  const ticket = db.prepare("SELECT * FROM tickets WHERE id = ?").get(req.params.id);
  if (!ticket) {
    return res.status(404).json({ error: "Ticket nicht gefunden" });
  }
  if (ticket.user_id !== req.user.userId) {
    return res.status(403).json({ error: "Keine Berechtigung" });
  }

  const deleteTicket = db.prepare("DELETE FROM tickets WHERE id = ?");
  const decrementAttendees = db.prepare(
    "UPDATE events SET attendees = max(0, attendees - 1) WHERE id = ?",
  );

  db.transaction(() => {
    deleteTicket.run(ticket.id);
    decrementAttendees.run(ticket.event_id);
  })();

  res.json({ success: true });
});

// POST /api/tickets/:id/checkin — an organizer validates/redeems a ticket for
// their own event (used by the QR scanner). Marks the ticket as "used" once.
router.post("/:id/checkin", authMiddleware, (req, res) => {
  const ticket = db
    .prepare(
      `SELECT tickets.*, events.organizer_id AS event_organizer_id, events.name AS event_name
       FROM tickets JOIN events ON events.id = tickets.event_id
       WHERE tickets.id = ?`,
    )
    .get(req.params.id);

  if (!ticket) return res.status(404).json({ error: "Ticket nicht gefunden" });
  if (ticket.event_organizer_id !== req.user.userId) {
    return res.status(403).json({ error: "Dieses Ticket gehört nicht zu deinem Event" });
  }
  if (ticket.status === "used") {
    return res.status(409).json({ error: "Ticket wurde bereits eingelöst" });
  }

  db.prepare("UPDATE tickets SET status = 'used' WHERE id = ?").run(ticket.id);
  res.json({ success: true, ticketId: ticket.id, eventName: ticket.event_name, status: "used" });
});

module.exports = router;
