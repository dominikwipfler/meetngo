const fs = require("fs");
const path = require("path");
const request = require("./request");
const app = require("../app");

// Der Tile-Proxy lädt bei einem Cache-Miss von einem externen Server. Damit die
// Tests offline und deterministisch bleiben, prüfen wir nur die Eingabe-
// Validierung (Koordinatenbereiche) — kein echter Netzwerkabruf nötig.
describe("tile proxy", () => {
  it("rejects out-of-range tile coordinates", async () => {
    // x liegt außerhalb [0, 2^z): bei z=1 sind nur 0 und 1 gültig.
    const res = await request(app).get("/tiles/1/5/0.png");
    expect(res.status).toBe(400);
  });

  it("rejects a zoom level beyond the allowed maximum", async () => {
    const res = await request(app).get("/tiles/30/0/0.png");
    expect(res.status).toBe(400);
  });

  it("rejects non-integer coordinates", async () => {
    const res = await request(app).get("/tiles/abc/0/0.png");
    expect(res.status).toBe(400);
  });

  it("rejects a negative zoom level (z < 0)", async () => {
    const res = await request(app).get("/tiles/-1/0/0.png");
    expect(res.status).toBe(400);
  });

  it("rejects a negative x coordinate", async () => {
    const res = await request(app).get("/tiles/1/-1/0.png");
    expect(res.status).toBe(400);
  });

  it("rejects a zoom exactly one beyond MAX_ZOOM (z=20)", async () => {
    // MAX_ZOOM is 19, so z=20 is the first invalid level.
    const res = await request(app).get("/tiles/20/0/0.png");
    expect(res.status).toBe(400);
  });

  it("rejects x/y at the upper bound 2^z (valid range is [0, 2^z))", async () => {
    // At z=1, 2^z = 2, so x=2 is the first out-of-range value.
    const resX = await request(app).get("/tiles/1/2/0.png");
    expect(resX.status).toBe(400);
    const resY = await request(app).get("/tiles/1/0/2.png");
    expect(resY.status).toBe(400);
  });

  it("rejects fractional coordinates", async () => {
    const res = await request(app).get("/tiles/2/1.5/1.png");
    expect(res.status).toBe(400);
  });

  // A previously-cached tile is served straight from disk with no network call,
  // so this stays offline-deterministic. backend/tiles-cache/ is gitignored, so
  // the fixture is seeded here instead of being shipped as a tracked file.
  it("serves a cached tile as a HIT without any upstream fetch", async () => {
    const cacheDir = path.join(__dirname, "..", "tiles-cache");
    fs.mkdirSync(cacheDir, { recursive: true });
    fs.writeFileSync(path.join(cacheDir, "14_8584_5595.png"), Buffer.from([0x89, 0x50, 0x4e, 0x47]));

    const res = await request(app).get("/tiles/14/8584/5595.png");
    expect(res.status).toBe(200);
    expect(res.headers["x-tile-cache"]).toBe("HIT");
    expect(res.headers["content-type"]).toBe("image/png");
  });
});
