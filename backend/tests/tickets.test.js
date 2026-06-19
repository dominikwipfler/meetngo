const request = require("supertest");
const app = require("../app");

async function registerUser(suffix) {
  const res = await request(app)
    .post("/api/auth/register")
    .send({
      username: `ticketuser_${suffix}`,
      email: `ticketuser_${suffix}@example.com`,
      password: "password123",
    });
  return { token: res.body.token, userId: res.body.user.id };
}

async function createEvent(token, overrides = {}) {
  let req = request(app)
    .post("/api/events")
    .set("Authorization", `Bearer ${token}`)
    .field("name", overrides.name ?? "Ticket Test Event")
    .field("date", "2026-01-01T10:00:00")
    .field("location", "Test Location");

  if (overrides.capacity != null) {
    req = req.field("capacity", String(overrides.capacity));
  }

  const res = await req;
  return res.body;
}

describe("POST /api/tickets", () => {
  it("rejects unauthenticated requests", async () => {
    const res = await request(app).post("/api/tickets").send({ eventId: 1 });
    expect(res.status).toBe(401);
  });

  it("rejects requests without an eventId", async () => {
    const { token } = await registerUser("buy1");
    const res = await request(app)
      .post("/api/tickets")
      .set("Authorization", `Bearer ${token}`)
      .send({});
    expect(res.status).toBe(400);
  });

  it("returns 404 for an unknown event", async () => {
    const { token } = await registerUser("buy2");
    const res = await request(app)
      .post("/api/tickets")
      .set("Authorization", `Bearer ${token}`)
      .send({ eventId: 999999 });
    expect(res.status).toBe(404);
  });

  it("creates a ticket and increments the event's attendee count", async () => {
    const organizer = await registerUser("organizer1");
    const event = await createEvent(organizer.token);

    const buyer = await registerUser("buy3");
    const res = await request(app)
      .post("/api/tickets")
      .set("Authorization", `Bearer ${buyer.token}`)
      .send({ eventId: event.id });

    expect(res.status).toBe(201);
    expect(res.body.event_id).toBe(event.id);
    expect(res.body.status).toBe("active");
    expect(res.body.event_name).toBe(event.name);

    const updatedEvent = await request(app).get(`/api/events/${event.id}`);
    expect(updatedEvent.body.attendees).toBe(1);
  });

  it("rejects purchases once the event is at capacity", async () => {
    const organizer = await registerUser("organizer2");
    const event = await createEvent(organizer.token, { capacity: 1 });

    const buyer1 = await registerUser("buy4");
    const first = await request(app)
      .post("/api/tickets")
      .set("Authorization", `Bearer ${buyer1.token}`)
      .send({ eventId: event.id });
    expect(first.status).toBe(201);

    const buyer2 = await registerUser("buy5");
    const second = await request(app)
      .post("/api/tickets")
      .set("Authorization", `Bearer ${buyer2.token}`)
      .send({ eventId: event.id });
    expect(second.status).toBe(409);
  });
});

describe("GET /api/tickets", () => {
  it("rejects unauthenticated requests", async () => {
    const res = await request(app).get("/api/tickets");
    expect(res.status).toBe(401);
  });

  it("only returns the authenticated user's own tickets", async () => {
    const organizer = await registerUser("organizer3");
    const event = await createEvent(organizer.token);

    const buyer = await registerUser("buy6");
    await request(app)
      .post("/api/tickets")
      .set("Authorization", `Bearer ${buyer.token}`)
      .send({ eventId: event.id });

    const otherUser = await registerUser("buy7");

    const buyerTickets = await request(app)
      .get("/api/tickets")
      .set("Authorization", `Bearer ${buyer.token}`);
    expect(buyerTickets.body.length).toBeGreaterThanOrEqual(1);
    expect(buyerTickets.body.every((t) => t.user_id === buyer.userId)).toBe(true);

    const otherTickets = await request(app)
      .get("/api/tickets")
      .set("Authorization", `Bearer ${otherUser.token}`);
    expect(otherTickets.body).toEqual([]);
  });
});
