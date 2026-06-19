const request = require("supertest");
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
});
