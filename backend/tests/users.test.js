const request = require("supertest");
const app = require("../app");

async function registerUser(suffix) {
  const res = await request(app)
    .post("/api/auth/register")
    .send({
      username: `profileuser_${suffix}`,
      email: `profileuser_${suffix}@example.com`,
      password: "password123",
    });
  return { token: res.body.token, userId: res.body.user.id };
}

describe("GET /api/users/me", () => {
  it("rejects unauthenticated requests", async () => {
    const res = await request(app).get("/api/users/me");
    expect(res.status).toBe(401);
  });

  it("returns the authenticated user's profile", async () => {
    const { token } = await registerUser("get1");
    const res = await request(app).get("/api/users/me").set("Authorization", `Bearer ${token}`);

    expect(res.status).toBe(200);
    expect(res.body).toMatchObject({
      username: "profileuser_get1",
      email: "profileuser_get1@example.com",
      interests: [],
    });
  });
});

describe("PATCH /api/users/me", () => {
  it("rejects unauthenticated requests", async () => {
    const res = await request(app).patch("/api/users/me").send({ username: "new" });
    expect(res.status).toBe(401);
  });

  it("updates username, email and interests", async () => {
    const { token } = await registerUser("patch1");
    const res = await request(app)
      .patch("/api/users/me")
      .set("Authorization", `Bearer ${token}`)
      .send({
        username: "patch1_renamed",
        email: "patch1_renamed@example.com",
        interests: ["Musik", "Sport"],
      });

    expect(res.status).toBe(200);
    expect(res.body).toMatchObject({
      username: "patch1_renamed",
      email: "patch1_renamed@example.com",
      interests: ["Musik", "Sport"],
    });
  });

  it("allows partial updates, leaving other fields untouched", async () => {
    const { token } = await registerUser("patch2");
    const res = await request(app)
      .patch("/api/users/me")
      .set("Authorization", `Bearer ${token}`)
      .send({ interests: ["Tech"] });

    expect(res.status).toBe(200);
    expect(res.body.username).toBe("profileuser_patch2");
    expect(res.body.email).toBe("profileuser_patch2@example.com");
    expect(res.body.interests).toEqual(["Tech"]);
  });

  it("rejects an empty username", async () => {
    const { token } = await registerUser("patch3");
    const res = await request(app)
      .patch("/api/users/me")
      .set("Authorization", `Bearer ${token}`)
      .send({ username: "   " });

    expect(res.status).toBe(400);
  });

  it("rejects an invalid email address", async () => {
    const { token } = await registerUser("patch4");
    const res = await request(app)
      .patch("/api/users/me")
      .set("Authorization", `Bearer ${token}`)
      .send({ email: "not-an-email" });

    expect(res.status).toBe(400);
  });

  it("rejects non-string interests", async () => {
    const { token } = await registerUser("patch5");
    const res = await request(app)
      .patch("/api/users/me")
      .set("Authorization", `Bearer ${token}`)
      .send({ interests: ["Musik", 42] });

    expect(res.status).toBe(400);
  });

  it("rejects a username already taken by another user", async () => {
    await registerUser("patch6a");
    const { token } = await registerUser("patch6b");

    const res = await request(app)
      .patch("/api/users/me")
      .set("Authorization", `Bearer ${token}`)
      .send({ username: "profileuser_patch6a" });

    expect(res.status).toBe(409);
  });

  it("rejects an email already taken by another user", async () => {
    await registerUser("patch7a");
    const { token } = await registerUser("patch7b");

    const res = await request(app)
      .patch("/api/users/me")
      .set("Authorization", `Bearer ${token}`)
      .send({ email: "profileuser_patch7a@example.com" });

    expect(res.status).toBe(409);
  });
});
