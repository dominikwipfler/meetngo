const request = require("./request");
const app = require("../app");

// The database seeds ~18 demo events on first connection (see database.js).
// Read-only list/sort/filter tests rely on that fixed, known seed data.

async function registerUser(suffix) {
  const res = await request(app)
    .post("/api/auth/register")
    .send({
      username: `organizer_${suffix}`,
      email: `organizer_${suffix}@example.com`,
      password: "password123",
    });
  return { token: res.body.token, userId: res.body.user.id };
}

describe("GET /api/events", () => {
  it("returns the seeded demo events", async () => {
    const res = await request(app).get("/api/events");
    expect(res.status).toBe(200);
    expect(res.body.length).toBeGreaterThan(0);
  });

  it("sorts by price numerically, not lexicographically", async () => {
    const res = await request(app).get("/api/events?sort=price&order=desc");
    expect(res.status).toBe(200);
    expect(res.body[0].name).toBe("Marathon 2026");
    expect(res.body[0].price_value).toBe(45);
  });

  it("sorts by price ascending with free events first", async () => {
    const res = await request(app).get("/api/events?sort=price&order=asc");
    expect(res.body[0].price_value).toBe(0);
  });

  it("filters by category", async () => {
    const res = await request(app).get("/api/events?category=Tech");
    expect(res.body.length).toBeGreaterThan(0);
    expect(res.body.every((e) => e.category === "Tech")).toBe(true);
  });

  it("filters free events", async () => {
    const res = await request(app).get("/api/events?priceFilter=free");
    expect(res.body.every((e) => e.price === "Kostenlos")).toBe(true);
  });

  it("filters paid events", async () => {
    const res = await request(app).get("/api/events?priceFilter=paid");
    expect(res.body.every((e) => e.price !== "Kostenlos")).toBe(true);
  });

  it("searches across name, location and description", async () => {
    const res = await request(app).get("/api/events?search=Jazz");
    expect(res.body.some((e) => e.name.includes("Jazz"))).toBe(true);
  });

  it("filters by a maximum price", async () => {
    const res = await request(app).get("/api/events?priceMax=10");
    expect(res.status).toBe(200);
    expect(res.body.length).toBeGreaterThan(0);
    expect(res.body.every((e) => e.price_value <= 10)).toBe(true);
    // Ein teures Event (Marathon, 45 €) darf hier nicht auftauchen.
    expect(res.body.some((e) => e.name === "Marathon 2026")).toBe(false);
  });

  it("filters by a date range (dateFrom/dateTo)", async () => {
    const res = await request(app).get("/api/events?dateFrom=2026-08-01T00:00:00&dateTo=2026-08-31T23:59:59");
    expect(res.status).toBe(200);
    expect(res.body.length).toBeGreaterThan(0);
    // Alle Treffer liegen im August 2026.
    expect(res.body.every((e) => e.date >= "2026-08-01" && e.date <= "2026-08-31T23:59:59")).toBe(true);
  });

  it("paginates with limit/offset and reports the full count in X-Total-Count", async () => {
    const all = await request(app).get("/api/events");
    const total = all.body.length;
    expect(total).toBeGreaterThan(5);

    const page1 = await request(app).get("/api/events?limit=5&offset=0");
    expect(page1.body.length).toBe(5);
    expect(page1.headers["x-total-count"]).toBe(String(total));

    const page2 = await request(app).get("/api/events?limit=5&offset=5");
    expect(page2.body.length).toBe(5);
    // Zweite Seite darf sich nicht mit der ersten überschneiden.
    const ids1 = page1.body.map((e) => e.id);
    expect(page2.body.every((e) => !ids1.includes(e.id))).toBe(true);
  });

  it("returns the full list (and count) when no limit is given", async () => {
    const res = await request(app).get("/api/events");
    expect(res.headers["x-total-count"]).toBe(String(res.body.length));
  });
});

describe("GET /api/events/:id", () => {
  it("returns 404 for an unknown id", async () => {
    const res = await request(app).get("/api/events/999999");
    expect(res.status).toBe(404);
  });

  it("returns the event for a known id", async () => {
    const list = await request(app).get("/api/events");
    const id = list.body[0].id;

    const res = await request(app).get(`/api/events/${id}`);
    expect(res.status).toBe(200);
    expect(res.body.id).toBe(id);
  });
});

describe("POST /api/events", () => {
  it("rejects unauthenticated requests", async () => {
    const res = await request(app)
      .post("/api/events")
      .send({ name: "Test", date: "2026-01-01", location: "Test" });
    expect(res.status).toBe(401);
  });

  it("rejects requests missing required fields", async () => {
    const { token } = await registerUser("create1");
    const res = await request(app)
      .post("/api/events")
      .set("Authorization", `Bearer ${token}`)
      .send({ name: "Missing date and location" });
    expect(res.status).toBe(400);
  });

  it("creates an event and computes price_value from the price string", async () => {
    const { token, userId } = await registerUser("create2");
    const res = await request(app)
      .post("/api/events")
      .set("Authorization", `Bearer ${token}`)
      .field("name", "Test Event")
      .field("date", "2026-01-01T10:00:00")
      .field("location", "Test Location")
      .field("price", "19,99");

    expect(res.status).toBe(201);
    expect(res.body.price_value).toBeCloseTo(19.99);
    expect(res.body.organizer_id).toBe(userId);
  });

  it("defaults to free when no price is given", async () => {
    const { token } = await registerUser("create3");
    const res = await request(app)
      .post("/api/events")
      .set("Authorization", `Bearer ${token}`)
      .field("name", "Free Event")
      .field("date", "2026-01-01T10:00:00")
      .field("location", "Test Location");

    expect(res.body.price).toBe("Kostenlos");
    expect(res.body.price_value).toBe(0);
  });
});

describe("DELETE /api/events/:id", () => {
  let token;
  let eventId;

  beforeAll(async () => {
    const auth = await registerUser("delete1");
    token = auth.token;
    const created = await request(app)
      .post("/api/events")
      .set("Authorization", `Bearer ${token}`)
      .field("name", "Deletable Event")
      .field("date", "2026-01-01T10:00:00")
      .field("location", "Test Location");
    eventId = created.body.id;
  });

  it("rejects unauthenticated requests", async () => {
    const res = await request(app).delete(`/api/events/${eventId}`);
    expect(res.status).toBe(401);
  });

  it("rejects deletion by a different user", async () => {
    const other = await registerUser("delete2");
    const res = await request(app)
      .delete(`/api/events/${eventId}`)
      .set("Authorization", `Bearer ${other.token}`);
    expect(res.status).toBe(403);
  });

  it("returns 404 for an unknown event", async () => {
    const res = await request(app)
      .delete("/api/events/999999")
      .set("Authorization", `Bearer ${token}`);
    expect(res.status).toBe(404);
  });

  it("deletes the event when the owner requests it", async () => {
    const res = await request(app)
      .delete(`/api/events/${eventId}`)
      .set("Authorization", `Bearer ${token}`);
    expect(res.status).toBe(200);

    const check = await request(app).get(`/api/events/${eventId}`);
    expect(check.status).toBe(404);
  });

  it("deletes an event that already has tickets and favorites (no FK error)", async () => {
    const owner = await registerUser("delete_cascade_owner");
    const buyer = await registerUser("delete_cascade_buyer");
    const created = await request(app)
      .post("/api/events")
      .set("Authorization", `Bearer ${owner.token}`)
      .field("name", "Cascade Event")
      .field("date", "2026-02-02T10:00:00")
      .field("location", "Test Location");
    const id = created.body.id;

    // A buyer takes a ticket and favorites the event, creating FK references.
    await request(app)
      .post("/api/tickets")
      .set("Authorization", `Bearer ${buyer.token}`)
      .send({ eventId: id });
    await request(app)
      .post(`/api/events/${id}/favorite`)
      .set("Authorization", `Bearer ${buyer.token}`);

    const res = await request(app)
      .delete(`/api/events/${id}`)
      .set("Authorization", `Bearer ${owner.token}`);
    expect(res.status).toBe(200);

    // The dependent ticket is gone too, so the buyer's ticket list no longer
    // references the deleted event.
    const tickets = await request(app)
      .get("/api/tickets")
      .set("Authorization", `Bearer ${buyer.token}`);
    expect(tickets.body.some((t) => t.event_id === id)).toBe(false);
  });
});

describe("PATCH /api/events/:id", () => {
  let token;
  let eventId;

  beforeAll(async () => {
    const auth = await registerUser("patch1");
    token = auth.token;
    const created = await request(app)
      .post("/api/events")
      .set("Authorization", `Bearer ${token}`)
      .field("name", "Featurable Event")
      .field("date", "2026-01-01T10:00:00")
      .field("location", "Test Location");
    eventId = created.body.id;
  });

  it("rejects unauthenticated requests", async () => {
    const res = await request(app).patch(`/api/events/${eventId}`).send({ featured: true });
    expect(res.status).toBe(401);
  });

  it("rejects updates by a different user", async () => {
    const other = await registerUser("patch2");
    const res = await request(app)
      .patch(`/api/events/${eventId}`)
      .set("Authorization", `Bearer ${other.token}`)
      .send({ featured: true });
    expect(res.status).toBe(403);
  });

  it("returns 404 for an unknown event", async () => {
    const res = await request(app)
      .patch("/api/events/999999")
      .set("Authorization", `Bearer ${token}`)
      .send({ featured: true });
    expect(res.status).toBe(404);
  });

  it("lets the owner toggle the featured flag", async () => {
    const on = await request(app)
      .patch(`/api/events/${eventId}`)
      .set("Authorization", `Bearer ${token}`)
      .send({ featured: true });
    expect(on.status).toBe(200);
    expect(on.body.featured).toBe(1);

    const off = await request(app)
      .patch(`/api/events/${eventId}`)
      .set("Authorization", `Bearer ${token}`)
      .send({ featured: false });
    expect(off.status).toBe(200);
    expect(off.body.featured).toBe(0);
  });

  it("hides deactivated events from the public list but keeps them for the organizer", async () => {
    const { token, userId } = await registerUser("active1");
    const created = await request(app)
      .post("/api/events")
      .set("Authorization", `Bearer ${token}`)
      .field("name", "Deactivatable Event")
      .field("date", "2026-01-01T10:00:00")
      .field("location", "Test Location");
    const id = created.body.id;

    const deactivated = await request(app)
      .patch(`/api/events/${id}`)
      .set("Authorization", `Bearer ${token}`)
      .send({ active: false });
    expect(deactivated.status).toBe(200);
    expect(deactivated.body.active).toBe(0);

    const publicList = await request(app).get("/api/events");
    expect(publicList.body.find((e) => e.id === id)).toBeUndefined();

    const organizerList = await request(app).get(`/api/events?organizerId=${userId}`);
    expect(organizerList.body.find((e) => e.id === id)).toBeDefined();

    const reactivated = await request(app)
      .patch(`/api/events/${id}`)
      .set("Authorization", `Bearer ${token}`)
      .send({ active: true });
    expect(reactivated.body.active).toBe(1);

    const publicAgain = await request(app).get("/api/events");
    expect(publicAgain.body.find((e) => e.id === id)).toBeDefined();
  });
});

describe("Favoriten", () => {
  it("rejects unauthenticated favorite requests", async () => {
    const res = await request(app).post("/api/events/1/favorite");
    expect(res.status).toBe(401);
  });

  it("returns 404 when favoriting an unknown event", async () => {
    const { token } = await registerUser("fav404");
    const res = await request(app)
      .post("/api/events/999999/favorite")
      .set("Authorization", `Bearer ${token}`);
    expect(res.status).toBe(404);
  });

  it("favorites and unfavorites an event and lists favorites", async () => {
    const { token } = await registerUser("fav1");
    const list = await request(app).get("/api/events");
    const eventId = list.body[0].id;

    const before = await request(app)
      .get(`/api/events/${eventId}/favorite-status`)
      .set("Authorization", `Bearer ${token}`);
    expect(before.body).toEqual({ favorited: false });

    const fav = await request(app)
      .post(`/api/events/${eventId}/favorite`)
      .set("Authorization", `Bearer ${token}`);
    expect(fav.body).toEqual({ favorited: true });

    const favorites = await request(app)
      .get("/api/events/favorites")
      .set("Authorization", `Bearer ${token}`);
    expect(favorites.body.find((e) => e.id === eventId)).toBeDefined();

    const status = await request(app)
      .get(`/api/events/${eventId}/favorite-status`)
      .set("Authorization", `Bearer ${token}`);
    expect(status.body).toEqual({ favorited: true });

    const unfav = await request(app)
      .delete(`/api/events/${eventId}/favorite`)
      .set("Authorization", `Bearer ${token}`);
    expect(unfav.body).toEqual({ favorited: false });

    const emptyFavorites = await request(app)
      .get("/api/events/favorites")
      .set("Authorization", `Bearer ${token}`);
    expect(emptyFavorites.body.find((e) => e.id === eventId)).toBeUndefined();
  });
});
