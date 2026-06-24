const request = require("./request");
const app = require("../app");

// Der Geocoding-Proxy ruft bei einer gültigen Anfrage einen externen Dienst
// (Nominatim) auf. Damit die Tests offline und deterministisch bleiben, prüfen
// wir nur die Eingabe-Validierung — kein echter Netzwerkabruf nötig.
describe("geocode proxy", () => {
  it("rejects a missing query", async () => {
    const res = await request(app).get("/api/geocode");
    expect(res.status).toBe(400);
  });

  it("rejects an empty/whitespace-only query", async () => {
    const res = await request(app).get("/api/geocode?q=%20%20");
    expect(res.status).toBe(400);
  });

  it("rejects an excessively long query", async () => {
    const res = await request(app).get(`/api/geocode?q=${"a".repeat(201)}`);
    expect(res.status).toBe(400);
  });
});
