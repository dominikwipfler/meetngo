const request = require("./request");
const app = require("../app");

describe("security headers", () => {
  it("sets common protective headers on responses", async () => {
    const res = await request(app).get("/api/events");
    expect(res.headers["x-content-type-options"]).toBe("nosniff");
    expect(res.headers["x-frame-options"]).toBe("DENY");
    expect(res.headers["referrer-policy"]).toBe("no-referrer");
    expect(res.headers["content-security-policy"]).toContain("default-src");
  });
});
