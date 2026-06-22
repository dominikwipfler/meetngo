const request = require("./request");
const app = require("../app");

// Additional edge/contract tests that fill gaps in the per-route suites. Each
// test reflects the route's *current intended* behavior, verified by reading
// the route source. Where current behavior is arguably surprising (e.g. a user
// may buy the same event twice), the test documents the contract as it stands.

let counter = 0;
async function registerUser(prefix) {
  counter += 1;
  const suffix = `${prefix}_${counter}`;
  const res = await request(app)
    .post("/api/auth/register")
    .send({
      username: `edge_${suffix}`,
      email: `edge_${suffix}@example.com`,
      password: "password123",
    });
  return { token: res.body.token, userId: res.body.user.id, username: `edge_${suffix}` };
}

async function createEvent(token, overrides = {}) {
  let req = request(app)
    .post("/api/events")
    .set("Authorization", `Bearer ${token}`)
    .field("name", overrides.name ?? "Edge Event")
    .field("date", overrides.date ?? "2026-01-01T10:00:00")
    .field("location", overrides.location ?? "Edge Location");
  if (overrides.capacity != null) req = req.field("capacity", String(overrides.capacity));
  if (overrides.category != null) req = req.field("category", overrides.category);
  const res = await req;
  return res.body;
}

describe("auth normalization", () => {
  it("trims the username and lowercases/trims the email on registration", async () => {
    const res = await request(app).post("/api/auth/register").send({
      username: "  PaddedName  ",
      email: "  MixedCase@Example.COM  ",
      password: "password123",
    });
    expect(res.status).toBe(201);
    expect(res.body.user.username).toBe("PaddedName");
    expect(res.body.user.email).toBe("mixedcase@example.com");
  });

  it("logs in case-insensitively against the stored (lowercased) email", async () => {
    await request(app).post("/api/auth/register").send({
      username: "CaseLogin",
      email: "caselogin@example.com",
      password: "password123",
    });
    const res = await request(app)
      .post("/api/auth/login")
      .send({ email: "CaseLogin@Example.com", password: "password123" });
    expect(res.status).toBe(200);
    expect(res.body.token).toBeTypeOf("string");
  });
});

describe("GET /api/events query handling", () => {
  it("falls back to the default sort for an unknown sort column", async () => {
    // An unrecognized sort must not error or allow SQL injection of the column.
    const res = await request(app).get("/api/events?sort=notacolumn");
    expect(res.status).toBe(200);
    expect(res.body.length).toBeGreaterThan(0);
  });

  it("returns an empty array (not an error) when a search matches nothing", async () => {
    const res = await request(app).get("/api/events?search=zzz_no_such_event_zzz");
    expect(res.status).toBe(200);
    expect(res.body).toEqual([]);
    expect(res.headers["x-total-count"]).toBe("0");
  });

  it("caps limit at 100 even when a larger limit is requested", async () => {
    const res = await request(app).get("/api/events?limit=9999");
    expect(res.status).toBe(200);
    expect(res.body.length).toBeLessThanOrEqual(100);
  });

  it("clamps a negative offset to 0 without erroring", async () => {
    const res = await request(app).get("/api/events?limit=2&offset=-5");
    expect(res.status).toBe(200);
    expect(res.body.length).toBe(2);
  });

  it("filters to a single organizer's events via organizerId", async () => {
    const organizer = await registerUser("orgfilter");
    const ev = await createEvent(organizer.token, { name: "Org Specific Event" });
    const res = await request(app).get(`/api/events?organizerId=${organizer.userId}`);
    expect(res.status).toBe(200);
    expect(res.body.every((e) => e.organizer_id === organizer.userId)).toBe(true);
    expect(res.body.find((e) => e.id === ev.id)).toBeDefined();
  });
});

describe("POST /api/events validation", () => {
  it("rejects a request missing only the location", async () => {
    const { token } = await registerUser("noloc");
    const res = await request(app)
      .post("/api/events")
      .set("Authorization", `Bearer ${token}`)
      .field("name", "Has name")
      .field("date", "2026-01-01T10:00:00");
    expect(res.status).toBe(400);
  });

  it("defaults the category to Sonstiges and assigns a fallback seed image", async () => {
    const { token } = await registerUser("defcat");
    const res = await request(app)
      .post("/api/events")
      .set("Authorization", `Bearer ${token}`)
      .field("name", "No Category Event")
      .field("date", "2026-01-01T10:00:00")
      .field("location", "Somewhere");
    expect(res.status).toBe(201);
    expect(res.body.category).toBe("Sonstiges");
    expect(res.body.image_path).toMatch(/^\/uploads\/seed\//);
  });
});

describe("POST /api/tickets edge cases", () => {
  it("accepts an eventId passed as a string (parsed to int)", async () => {
    const organizer = await registerUser("strorg");
    const event = await createEvent(organizer.token);
    const buyer = await registerUser("strbuy");
    const res = await request(app)
      .post("/api/tickets")
      .set("Authorization", `Bearer ${buyer.token}`)
      .send({ eventId: String(event.id) });
    expect(res.status).toBe(201);
    expect(res.body.event_id).toBe(event.id);
  });

  it("treats eventId 0 as missing (400)", async () => {
    // parseInt("0") is falsy in the route's `if (!eventId)` guard.
    const { token } = await registerUser("zeroid");
    const res = await request(app)
      .post("/api/tickets")
      .set("Authorization", `Bearer ${token}`)
      .send({ eventId: 0 });
    expect(res.status).toBe(400);
  });

  it("allows unlimited purchases when the event has no capacity set", async () => {
    const organizer = await registerUser("uncaporg");
    const event = await createEvent(organizer.token); // capacity null = unlimited
    for (let i = 0; i < 3; i++) {
      const buyer = await registerUser(`uncapbuy${i}`);
      const res = await request(app)
        .post("/api/tickets")
        .set("Authorization", `Bearer ${buyer.token}`)
        .send({ eventId: event.id });
      expect(res.status).toBe(201);
    }
    const updated = await request(app).get(`/api/events/${event.id}`);
    expect(updated.body.attendees).toBe(3);
  });

  it("rejects buying a second ticket for the same event and keeps attendees at 1", async () => {
    // A user may hold at most one ticket per event: the first purchase succeeds,
    // a second returns 409 and must not increment the attendee count again.
    const organizer = await registerUser("duporg");
    const event = await createEvent(organizer.token);
    const buyer = await registerUser("dupbuy");
    const first = await request(app)
      .post("/api/tickets")
      .set("Authorization", `Bearer ${buyer.token}`)
      .send({ eventId: event.id });
    const second = await request(app)
      .post("/api/tickets")
      .set("Authorization", `Bearer ${buyer.token}`)
      .send({ eventId: event.id });
    expect(first.status).toBe(201);
    expect(second.status).toBe(409);
    expect(second.body.error).toMatch(/bereits ein Ticket/i);
    const updated = await request(app).get(`/api/events/${event.id}`);
    expect(updated.body.attendees).toBe(1);
  });

  it("returns the authenticated user's tickets sorted by event date ascending", async () => {
    const organizer = await registerUser("sortorg");
    const later = await createEvent(organizer.token, { date: "2026-12-31T10:00:00", name: "Later" });
    const sooner = await createEvent(organizer.token, { date: "2026-01-02T10:00:00", name: "Sooner" });
    const buyer = await registerUser("sortbuy");
    await request(app)
      .post("/api/tickets")
      .set("Authorization", `Bearer ${buyer.token}`)
      .send({ eventId: later.id });
    await request(app)
      .post("/api/tickets")
      .set("Authorization", `Bearer ${buyer.token}`)
      .send({ eventId: sooner.id });

    const res = await request(app).get("/api/tickets").set("Authorization", `Bearer ${buyer.token}`);
    const mine = res.body.filter((t) => t.event_id === later.id || t.event_id === sooner.id);
    expect(mine.map((t) => t.event_id)).toEqual([sooner.id, later.id]);
  });
});

describe("favorites idempotency and unknown targets", () => {
  it("is idempotent when favoriting twice", async () => {
    const organizer = await registerUser("favorg");
    const event = await createEvent(organizer.token);
    const { token } = await registerUser("favidem");

    const first = await request(app)
      .post(`/api/events/${event.id}/favorite`)
      .set("Authorization", `Bearer ${token}`);
    const second = await request(app)
      .post(`/api/events/${event.id}/favorite`)
      .set("Authorization", `Bearer ${token}`);
    expect(first.body).toEqual({ favorited: true });
    expect(second.body).toEqual({ favorited: true });

    const favorites = await request(app)
      .get("/api/events/favorites")
      .set("Authorization", `Bearer ${token}`);
    expect(favorites.body.filter((e) => e.id === event.id).length).toBe(1);
  });

  it("treats unfavoriting a non-favorited/unknown event as a no-op success", async () => {
    const { token } = await registerUser("unfavnoop");
    const res = await request(app)
      .delete("/api/events/999999/favorite")
      .set("Authorization", `Bearer ${token}`);
    expect(res.status).toBe(200);
    expect(res.body).toEqual({ favorited: false });
  });

  it("requires authentication for favorite-status and favorites list", async () => {
    const statusRes = await request(app).get("/api/events/1/favorite-status");
    expect(statusRes.status).toBe(401);
    const listRes = await request(app).get("/api/events/favorites");
    expect(listRes.status).toBe(401);
  });
});

describe("users profile/follow edge cases", () => {
  it("lowercases and trims an email updated via PATCH /api/users/me", async () => {
    const { token } = await registerUser("meemail");
    const res = await request(app)
      .patch("/api/users/me")
      .set("Authorization", `Bearer ${token}`)
      .send({ email: "  UPPER@Mail.COM " });
    expect(res.status).toBe(200);
    expect(res.body.email).toBe("upper@mail.com");
  });

  it("allows re-saving the user's own current username without a conflict", async () => {
    const user = await registerUser("selfname");
    const res = await request(app)
      .patch("/api/users/me")
      .set("Authorization", `Bearer ${user.token}`)
      .send({ username: user.username });
    expect(res.status).toBe(200);
    expect(res.body.username).toBe(user.username);
  });

  it("rejects a non-numeric organizer id on follow-status (400)", async () => {
    const { token } = await registerUser("followbadid");
    const res = await request(app)
      .get("/api/users/abc/follow-status")
      .set("Authorization", `Bearer ${token}`);
    expect(res.status).toBe(400);
  });

  it("requires authentication for follow-status", async () => {
    const res = await request(app).get("/api/users/1/follow-status");
    expect(res.status).toBe(401);
  });

  it("is idempotent when unfollowing someone not followed", async () => {
    const organizer = await registerUser("ufollowtarget");
    const fan = await registerUser("ufollowfan");
    const res = await request(app)
      .delete(`/api/users/${organizer.userId}/follow`)
      .set("Authorization", `Bearer ${fan.token}`);
    expect(res.status).toBe(200);
    expect(res.body).toEqual({ following: false, followers: 0 });
  });
});
