const request = require("./request");
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

describe("Follow / Abonnenten", () => {
  it("rejects unauthenticated follow", async () => {
    const res = await request(app).post("/api/users/1/follow");
    expect(res.status).toBe(401);
  });

  it("rejects following yourself", async () => {
    const { token, userId } = await registerUser("follow_self");
    const res = await request(app)
      .post(`/api/users/${userId}/follow`)
      .set("Authorization", `Bearer ${token}`);
    expect(res.status).toBe(400);
  });

  it("returns 404 for an unknown organizer", async () => {
    const { token } = await registerUser("follow_404");
    const res = await request(app)
      .post("/api/users/999999/follow")
      .set("Authorization", `Bearer ${token}`);
    expect(res.status).toBe(404);
  });

  it("follows and unfollows, tracking the follower count", async () => {
    const organizer = await registerUser("follow_org");
    const fan = await registerUser("follow_fan");

    const before = await request(app)
      .get(`/api/users/${organizer.userId}/follow-status`)
      .set("Authorization", `Bearer ${fan.token}`);
    expect(before.body).toEqual({ following: false, followers: 0 });

    const followed = await request(app)
      .post(`/api/users/${organizer.userId}/follow`)
      .set("Authorization", `Bearer ${fan.token}`);
    expect(followed.status).toBe(200);
    expect(followed.body).toEqual({ following: true, followers: 1 });

    // Idempotent: following again does not double-count.
    const again = await request(app)
      .post(`/api/users/${organizer.userId}/follow`)
      .set("Authorization", `Bearer ${fan.token}`);
    expect(again.body.followers).toBe(1);

    const unfollowed = await request(app)
      .delete(`/api/users/${organizer.userId}/follow`)
      .set("Authorization", `Bearer ${fan.token}`);
    expect(unfollowed.body).toEqual({ following: false, followers: 0 });
  });
});

describe("PATCH /api/users/me/password", () => {
  it("rejects unauthenticated requests", async () => {
    const res = await request(app)
      .patch("/api/users/me/password")
      .send({ currentPassword: "password123", newPassword: "newpass123" });
    expect(res.status).toBe(401);
  });

  it("rejects a wrong current password", async () => {
    const { token } = await registerUser("pw_wrong");
    const res = await request(app)
      .patch("/api/users/me/password")
      .set("Authorization", `Bearer ${token}`)
      .send({ currentPassword: "totally-wrong", newPassword: "newpass123" });
    expect(res.status).toBe(401);
  });

  it("rejects a too-short new password", async () => {
    const { token } = await registerUser("pw_short");
    const res = await request(app)
      .patch("/api/users/me/password")
      .set("Authorization", `Bearer ${token}`)
      .send({ currentPassword: "password123", newPassword: "123" });
    expect(res.status).toBe(400);
  });

  it("changes the password and lets the user log in with the new one", async () => {
    const suffix = "pw_ok";
    const { token } = await registerUser(suffix);
    const email = `profileuser_${suffix}@example.com`;

    const change = await request(app)
      .patch("/api/users/me/password")
      .set("Authorization", `Bearer ${token}`)
      .send({ currentPassword: "password123", newPassword: "brandnew123" });
    expect(change.status).toBe(200);

    const oldLogin = await request(app)
      .post("/api/auth/login")
      .send({ email, password: "password123" });
    expect(oldLogin.status).toBe(401);

    const newLogin = await request(app)
      .post("/api/auth/login")
      .send({ email, password: "brandnew123" });
    expect(newLogin.status).toBe(200);
  });
});
