const { rateLimit } = require("../middleware/rateLimit");

// Die Middleware deaktiviert sich in Tests selbst (NODE_ENV === "test"). Um das
// echte Limit-Verhalten zu prüfen, übersteuern wir das per `enabled: true` —
// ohne process.env global zu verändern (das würde in andere Test-Dateien leaken).
describe("rateLimit middleware", () => {
  function mockRes() {
    return {
      statusCode: 200,
      body: null,
      headers: {},
      status(code) {
        this.statusCode = code;
        return this;
      },
      json(payload) {
        this.body = payload;
        return this;
      },
      set(name, value) {
        this.headers[name] = value;
        return this;
      },
    };
  }

  it("allows up to max requests then returns 429", () => {
    const limiter = rateLimit({ windowMs: 60_000, max: 3, message: "Stop", enabled: true });
    // Eindeutige IP, damit dieser Test keine fremden Buckets trifft.
    const req = { ip: "10.1.1.1", baseUrl: "/api/auth", path: "/login" };

    let allowed = 0;
    let blocked = null;
    for (let i = 0; i < 5; i++) {
      const res = mockRes();
      let nextCalled = false;
      limiter(req, res, () => {
        nextCalled = true;
      });
      if (nextCalled) allowed++;
      else blocked = res;
    }

    expect(allowed).toBe(3);
    expect(blocked.statusCode).toBe(429);
    expect(blocked.body.error).toBe("Stop");
    expect(blocked.headers["Retry-After"]).toBeDefined();
  });

  it("isolates limits per IP", () => {
    const limiter = rateLimit({ windowMs: 60_000, max: 1, enabled: true });
    const resA = mockRes();
    const resB = mockRes();
    let aNext = false;
    let bNext = false;

    limiter({ ip: "10.2.2.2", baseUrl: "/api/auth", path: "/login" }, resA, () => {
      aNext = true;
    });
    limiter({ ip: "10.3.3.3", baseUrl: "/api/auth", path: "/login" }, resB, () => {
      bNext = true;
    });

    expect(aNext).toBe(true);
    expect(bNext).toBe(true);
  });
});
