const request = require("./request");
const jwt = require("jsonwebtoken");
const app = require("../app");
const { JWT_SECRET } = require("../middleware/auth");

// The auth middleware guards every protected route. The existing suites cover
// the "no Authorization header -> 401" path; here we exercise the remaining
// branches: malformed header, garbage token, expired token, and a token signed
// with the wrong secret. We use a protected GET (GET /api/users/me) as a
// representative protected endpoint.
const PROTECTED = "/api/users/me";

describe("auth middleware (JWT edge cases)", () => {
  it("rejects a missing Authorization header", async () => {
    const res = await request(app).get(PROTECTED);
    expect(res.status).toBe(401);
  });

  it("rejects an Authorization header without a Bearer token", async () => {
    // "Bearer" with no token -> split(" ")[1] is undefined -> treated as missing.
    const res = await request(app).get(PROTECTED).set("Authorization", "Bearer");
    expect(res.status).toBe(401);
  });

  it("rejects a malformed (non-JWT) token", async () => {
    const res = await request(app).get(PROTECTED).set("Authorization", "Bearer not.a.jwt");
    expect(res.status).toBe(401);
    expect(res.body.error).toBe("Ungültiger Token");
  });

  it("rejects a token signed with the wrong secret", async () => {
    const forged = jwt.sign({ userId: 1, username: "mallory" }, "the-wrong-secret", {
      expiresIn: "7d",
    });
    const res = await request(app).get(PROTECTED).set("Authorization", `Bearer ${forged}`);
    expect(res.status).toBe(401);
    expect(res.body.error).toBe("Ungültiger Token");
  });

  it("rejects an expired token", async () => {
    // Signed with the correct secret but already past its expiry.
    const expired = jwt.sign({ userId: 1, username: "alice" }, JWT_SECRET, { expiresIn: "-1s" });
    const res = await request(app).get(PROTECTED).set("Authorization", `Bearer ${expired}`);
    expect(res.status).toBe(401);
    expect(res.body.error).toBe("Ungültiger Token");
  });

  it("accepts a valid, correctly-signed token", async () => {
    const reg = await request(app).post("/api/auth/register").send({
      username: "jwt_valid_user",
      email: "jwt_valid_user@example.com",
      password: "password123",
    });
    const res = await request(app)
      .get(PROTECTED)
      .set("Authorization", `Bearer ${reg.body.token}`);
    expect(res.status).toBe(200);
    expect(res.body.username).toBe("jwt_valid_user");
  });
});

describe("unknown /api routes", () => {
  it("returns a JSON 404 for an unknown API path", async () => {
    const res = await request(app).get("/api/this-route-does-not-exist");
    expect(res.status).toBe(404);
    expect(res.body).toEqual({ error: "Nicht gefunden" });
  });

  it("returns a JSON 404 for an unknown API path under a known prefix", async () => {
    const res = await request(app).post("/api/auth/logout").send({});
    expect(res.status).toBe(404);
    expect(res.body).toEqual({ error: "Nicht gefunden" });
  });
});
