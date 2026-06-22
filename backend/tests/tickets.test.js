const request = require("./request");
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

  it("rejects purchases for a deactivated event", async () => {
    const organizer = await registerUser("organizer_inactive");
    const event = await createEvent(organizer.token, {});
    await request(app)
      .patch(`/api/events/${event.id}`)
      .set("Authorization", `Bearer ${organizer.token}`)
      .send({ active: false });

    const buyer = await registerUser("buy_inactive");
    const res = await request(app)
      .post("/api/tickets")
      .set("Authorization", `Bearer ${buyer.token}`)
      .send({ eventId: event.id });
    expect(res.status).toBe(409);
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

describe("DELETE /api/tickets/:id", () => {
  it("rejects unauthenticated requests", async () => {
    const res = await request(app).delete("/api/tickets/1");
    expect(res.status).toBe(401);
  });

  it("returns 404 for an unknown ticket", async () => {
    const { token } = await registerUser("cancel1");
    const res = await request(app)
      .delete("/api/tickets/999999")
      .set("Authorization", `Bearer ${token}`);
    expect(res.status).toBe(404);
  });

  it("rejects cancellation by a different user", async () => {
    const organizer = await registerUser("organizer4");
    const event = await createEvent(organizer.token);
    const buyer = await registerUser("cancel2");
    const bought = await request(app)
      .post("/api/tickets")
      .set("Authorization", `Bearer ${buyer.token}`)
      .send({ eventId: event.id });

    const other = await registerUser("cancel3");
    const res = await request(app)
      .delete(`/api/tickets/${bought.body.id}`)
      .set("Authorization", `Bearer ${other.token}`);
    expect(res.status).toBe(403);
  });

  it("cancels the owner's ticket and frees up an attendee slot", async () => {
    const organizer = await registerUser("organizer5");
    const event = await createEvent(organizer.token);
    const buyer = await registerUser("cancel4");
    const bought = await request(app)
      .post("/api/tickets")
      .set("Authorization", `Bearer ${buyer.token}`)
      .send({ eventId: event.id });

    const afterBuy = await request(app).get(`/api/events/${event.id}`);
    expect(afterBuy.body.attendees).toBe(1);

    const res = await request(app)
      .delete(`/api/tickets/${bought.body.id}`)
      .set("Authorization", `Bearer ${buyer.token}`);
    expect(res.status).toBe(200);

    const afterCancel = await request(app).get(`/api/events/${event.id}`);
    expect(afterCancel.body.attendees).toBe(0);

    const myTickets = await request(app)
      .get("/api/tickets")
      .set("Authorization", `Bearer ${buyer.token}`);
    expect(myTickets.body.find((t) => t.id === bought.body.id)).toBeUndefined();
  });
});

describe("POST /api/tickets/:id/checkin", () => {
  it("rejects unauthenticated requests", async () => {
    const res = await request(app).post("/api/tickets/1/checkin");
    expect(res.status).toBe(401);
  });

  it("returns 404 for an unknown ticket", async () => {
    const { token } = await registerUser("checkin404");
    const res = await request(app)
      .post("/api/tickets/999999/checkin")
      .set("Authorization", `Bearer ${token}`);
    expect(res.status).toBe(404);
  });

  it("rejects check-in by someone who is not the event organizer", async () => {
    const organizer = await registerUser("checkin_org1");
    const event = await createEvent(organizer.token);
    const buyer = await registerUser("checkin_buyer1");
    const bought = await request(app)
      .post("/api/tickets")
      .set("Authorization", `Bearer ${buyer.token}`)
      .send({ eventId: event.id });

    // The buyer (not the organizer) tries to check in their own ticket.
    const res = await request(app)
      .post(`/api/tickets/${bought.body.id}/checkin`)
      .set("Authorization", `Bearer ${buyer.token}`);
    expect(res.status).toBe(403);
  });

  it("lets the organizer redeem a ticket once, then rejects re-use", async () => {
    const organizer = await registerUser("checkin_org2");
    const event = await createEvent(organizer.token);
    const buyer = await registerUser("checkin_buyer2");
    const bought = await request(app)
      .post("/api/tickets")
      .set("Authorization", `Bearer ${buyer.token}`)
      .send({ eventId: event.id });

    const first = await request(app)
      .post(`/api/tickets/${bought.body.id}/checkin`)
      .set("Authorization", `Bearer ${organizer.token}`);
    expect(first.status).toBe(200);
    expect(first.body.status).toBe("used");

    const second = await request(app)
      .post(`/api/tickets/${bought.body.id}/checkin`)
      .set("Authorization", `Bearer ${organizer.token}`);
    expect(second.status).toBe(409);
  });
});
