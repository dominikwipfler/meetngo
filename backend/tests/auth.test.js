const request = require("supertest");
const app = require("../app");

describe("POST /api/auth/register", () => {
  it("creates a new user and returns a token", async () => {
    const res = await request(app).post("/api/auth/register").send({
      username: "alice",
      email: "alice@example.com",
      password: "password123",
    });

    expect(res.status).toBe(201);
    expect(res.body.token).toBeTypeOf("string");
    expect(res.body.user).toMatchObject({ username: "alice", email: "alice@example.com" });
  });

  it("rejects missing fields", async () => {
    const res = await request(app)
      .post("/api/auth/register")
      .send({ username: "bob", email: "bob@example.com" });

    expect(res.status).toBe(400);
  });

  it("rejects passwords shorter than 6 characters", async () => {
    const res = await request(app)
      .post("/api/auth/register")
      .send({ username: "bob", email: "bob@example.com", password: "123" });

    expect(res.status).toBe(400);
  });

  it("rejects invalid email addresses", async () => {
    const res = await request(app)
      .post("/api/auth/register")
      .send({ username: "bob", email: "not-an-email", password: "password123" });

    expect(res.status).toBe(400);
  });

  it("rejects duplicate usernames", async () => {
    await request(app)
      .post("/api/auth/register")
      .send({ username: "carol", email: "carol@example.com", password: "password123" });

    const res = await request(app)
      .post("/api/auth/register")
      .send({ username: "carol", email: "different@example.com", password: "password123" });

    expect(res.status).toBe(409);
  });

  it("rejects duplicate email addresses", async () => {
    await request(app)
      .post("/api/auth/register")
      .send({ username: "dave1", email: "dave@example.com", password: "password123" });

    const res = await request(app)
      .post("/api/auth/register")
      .send({ username: "dave2", email: "dave@example.com", password: "password123" });

    expect(res.status).toBe(409);
  });
});

describe("POST /api/auth/login", () => {
  it("logs in with correct credentials", async () => {
    await request(app)
      .post("/api/auth/register")
      .send({ username: "erin", email: "erin@example.com", password: "password123" });

    const res = await request(app)
      .post("/api/auth/login")
      .send({ email: "erin@example.com", password: "password123" });

    expect(res.status).toBe(200);
    expect(res.body.token).toBeTypeOf("string");
  });

  it("rejects wrong password", async () => {
    await request(app)
      .post("/api/auth/register")
      .send({ username: "frank", email: "frank@example.com", password: "password123" });

    const res = await request(app)
      .post("/api/auth/login")
      .send({ email: "frank@example.com", password: "wrongpassword" });

    expect(res.status).toBe(401);
  });

  it("rejects unknown email", async () => {
    const res = await request(app)
      .post("/api/auth/login")
      .send({ email: "nobody@example.com", password: "password123" });

    expect(res.status).toBe(401);
  });

  it("rejects missing fields", async () => {
    const res = await request(app).post("/api/auth/login").send({ email: "erin@example.com" });

    expect(res.status).toBe(400);
  });
});
